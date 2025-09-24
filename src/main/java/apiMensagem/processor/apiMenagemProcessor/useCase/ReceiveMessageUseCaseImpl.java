package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.MetaWhatsAppWebhookPayload;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import apiMensagem.processor.apiMenagemProcessor.gateway.ApiProcessorGateway;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayImpl;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayMetaImpl;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveMessageUseCaseImpl implements ReceiveMessageUseCase {

    private final OrganizationRepository organizationRepository;
    private final ApiProcessorGateway apiProcessorGateway;
    private final WhatsAppGatewayImpl whatsAppGatewayEvolution;
    private final WhatsAppGatewayMetaImpl whatsAppGatewayMeta;

    @Value("${processor.limitAudio}")
    private Integer limitAudio;

    @Override
    public void receiveMessage(WebhookMessagePayload payload) {
        try {
            if (payload == null || payload.getData() == null) {
                log.warn("Payload inválido ou nulo.");
                return;
            }

            String messageType = payload.getData().getMessageType();
            String orgId = payload.getInstance();
            String remoteJid = payload.getData().getKey() != null ? payload.getData().getKey().getRemoteJid() : null;

            if (remoteJid == null || orgId == null) {
                log.warn("Dados incompletos no payload.");
                return;
            }

            remoteJid = remoteJid.replace("@s.whatsapp.net", "");

            var org = organizationRepository.findByorgId(orgId)
                    .orElseThrow(FileSystemNotFoundException::new);

            String token = org.token();
            String instanceName = org.instanceName();

            switch (messageType) {
                case "conversation":
                    String conversation = payload.getData().getMessage().getConversation();
                    String contactName = payload.getData().getPushName() != null ? payload.getData().getPushName() : "Desconhecido";
                    apiProcessorGateway.sendTextMessage(remoteJid, orgId, conversation, contactName);
                    log.info("Mensagem de texto enviada: [{}] - [{}]", remoteJid, conversation);
                    break;

                case "audioMessage":
                    var audioMessage = payload.getData().getMessage().getAudioMessage();
                    String audioUrl = audioMessage.getUrl();
                    String contactNameAudio = payload.getData().getPushName() != null ? payload.getData().getPushName() : "Desconhecido";
                    if (audioMessage.getSeconds() < limitAudio) {
                        if (audioUrl != null) {
                            apiProcessorGateway.sendAudioMessage(remoteJid, orgId, audioUrl, audioMessage.getMimetype(), audioMessage.getMediaKey(), contactNameAudio);
                            log.info("Áudio enviado: [{}] - [{}]", remoteJid, audioUrl);
                        } else {
                            log.warn("URL do áudio ausente.");
                        }
                    } else {
                        String mensagemPadrao = "Recebemos seu áudio! 😊 Para conseguirmos te ajudar melhor, envie áudios com até " + limitAudio + " segundos.";
                        whatsAppGatewayEvolution.sendMessage(remoteJid, mensagemPadrao, token, instanceName);
                    }
                    break;

                case "imageMessage":
                case "videoMessage":
                    String mensagemDesculpa = "Desculpe, ainda não entendi esse tipo de mensagem. No momento só aceitamos mensagens de texto ou áudio.";
                    whatsAppGatewayEvolution.sendMessage(remoteJid, mensagemDesculpa, token, instanceName);
                    log.info("Mensagem de tipo não suportado respondida para [{}] - [{}]", remoteJid, mensagemDesculpa);
                    break;

                default:
                    log.info("Mensagem ignorada. Tipo não processado: {}", messageType);
                    break;
            }

        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
        }
    }

    @Override
    public void receiveStatusMessageMeta(MetaWhatsAppWebhookPayload payload) {
        try {
            if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
                log.warn("Payload Meta inválido ou nulo.");
                return;
            }

            var firstEntry = payload.getEntry().getFirst();
            if (Objects.isNull(firstEntry.getChanges().getFirst().getValue().getMessages()) || firstEntry.getChanges().getFirst().getValue().getMessages().isEmpty()) {
                log.info("Sem mensagens para processar nesta change. mensagem vinda da IA");
                return;
            }

            // Percorre todas as entries/changes/messages (Meta pode agrupar)
            for (MetaWhatsAppWebhookPayload.Entry entry : payload.getEntry()) {
                if (entry == null || entry.getChanges() == null) continue;

                var change = entry.getChanges().getFirst();
                    if (change == null || change.getValue() == null) continue;

                    MetaWhatsAppWebhookPayload.Value value = change.getValue();
                    MetaWhatsAppWebhookPayload.Metadata metadata = value.getMetadata();

                    if (metadata == null || metadata.getPhoneNumberId() == null) {
                        log.warn("Metadata ausente ou sem phone_number_id no payload Meta.");
                        continue;
                    }

                    // >>> Ajuste este lookup conforme seu repository
                    var org = resolveOrganizationByPhoneNumberId(payload.getEntry().getFirst().getId());
                    if (org == null) {
                        log.warn("Organização não encontrada para phone_number_id={}.", metadata.getPhoneNumberId());
                        continue;
                    }

                    String token = org.token();
                    String instanceName = org.instanceName();

                    List<MetaWhatsAppWebhookPayload.Message> messages = value.getMessages();
                    if (messages == null || messages.isEmpty()) {
                        log.info("Sem mensagens para processar nesta change.");
                        continue;
                    }

                    // Nome do contato (se existir em contacts[0].profile.name)
                    String contactName = extractContactName(value);

                    for (MetaWhatsAppWebhookPayload.Message msg : messages) {
                        if (msg == null || msg.getType() == null) {
                            log.info("Mensagem Meta nula ou sem type; ignorando.");
                            continue;
                        }

                        // No Cloud API, o 'from' é o número do cliente (sem @s.whatsapp.net)
                        String remoteJid = msg.getFrom();
                        if (remoteJid == null || remoteJid.isBlank()) {
                            log.warn("Mensagem sem 'from' (remoteJid) no Meta; ignorando.");
                            continue;
                        }

                        switch (msg.getType()) {
                            case "text" -> {
                                var text = msg.getText();
                                String body = (text != null) ? text.getBody() : null;
                                if (body == null || body.isBlank()) {
                                    log.info("Texto vazio recebido; ignorando.");
                                    continue;
                                }
                                // No seu método original você usa orgId; aqui usamos phone_number_id como chave da org.
                                apiProcessorGateway.sendTextMessage(remoteJid, org.orgId(), body, contactName);
                                log.info("Mensagem de texto enviada (Meta): [{}] - [{}]", remoteJid, body);
                            }

                            case "audio" -> {
                                var audio = msg.getAudio();
                                if (audio == null || audio.getId() == null) {
                                    log.warn("Áudio sem media-id no Meta; ignorando.");
                                    continue;
                                }

                                String msgPadrao = "Recebemos seu áudio! 😊 Porém não consegui processá-lo agora. Por favor, envie em formato de texto.";
                                whatsAppGatewayMeta.sendMessage(remoteJid, msgPadrao, token, instanceName, org.numberIdMeta());

                                // OBS: O webhook do Meta NÃO traz duração do áudio.
                                // Se você precisa impor 'limitAudio', considere pedir ao usuário para
                                // regravar curto, ou aceitar o áudio sem checagem de segundos aqui.
                                // Abaixo tentamos resolver uma URL temporária do Graph para encaminhar.
//                                String mediaId = audio.getId();
//                                String mimetype = audio.getMimeType();
//                                String audioUrl = getMetaMediaUrl(mediaId, token); // URL temporária (GET /{media-id})
//
//                                if (audioUrl != null) {
//                                    // Seu método atual recebe (remoteJid, orgId, url, mimetype, mediaKey, contactName)
//                                    // No Meta não há 'mediaKey'. Passamos null.
//                                    apiProcessorGateway.sendAudioMessage(remoteJid, org.orgId(), audioUrl, mimetype, "teste", contactName);
//                                    log.info("Áudio (Meta) encaminhado: [{}] - [mediaId={}]", remoteJid, mediaId);
//                                } else {
////                                    String msgPadrao = "Recebemos seu áudio! 😊 Porém não consegui processá-lo agora. Por favor, tente reenviar.";
////                                    whatsAppGateway.sendMessage(remoteJid, msgPadrao, token, instanceName);
//                                    log.warn("Falha ao obter URL do áudio (Meta) mediaId={} para [{}].", mediaId, remoteJid);
//                                }
                            }

                            case "image", "video" -> {
                                String mensagemDesculpa = "Desculpe, ainda não entendi esse tipo de mensagem. No momento só aceitamos mensagens de texto ou áudio.";
                                whatsAppGatewayMeta.sendMessage(remoteJid, mensagemDesculpa, token, instanceName, org.numberIdMeta());
                                log.info("Mensagem não suportada ({} - Meta) respondida para [{}].", msg.getType(), remoteJid);
                            }

                            default -> {
                                log.info("Mensagem ignorada. Tipo (Meta) não processado: {}", msg.getType());
                            }
                        }
                    }
                }
        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida (Meta): {}", e.getMessage(), e);
        }
    }

    /* ======================= Helpers (mesma classe) ======================= */

    private OrganizationsEntity resolveOrganizationByPhoneNumberId(String idMeta) {
        // Ajuste conforme seu repository. Ex.: findByPhoneNumberId, findByInstanceName, etc.
        // Se seu domínio usa orgId como 'instance', mapeie phoneNumberId->orgId numa tabela.
        return organizationRepository.findByIdMeta(idMeta).orElse(null);
    }

    private String extractContactName(MetaWhatsAppWebhookPayload.Value value) {
        try {
            if (value.getContacts() != null && !value.getContacts().isEmpty()) {
                var c = value.getContacts().get(0);
                if (c != null && c.getProfile() != null && c.getProfile().getName() != null && !c.getProfile().getName().isBlank()) {
                    return c.getProfile().getName();
                }
            }
        } catch (Exception ignored) {
        }
        return "Desconhecido";
    }

    /**
     * Obtém a URL temporária do media do Meta (Cloud API) usando o media-id.
     * 1) GET https://graph.facebook.com/v20.0/{media-id}  (Authorization: Bearer {token})
     * → { "url": "https://..." , "mime_type": "...", ... }
     * 2) A URL retornada exige novo GET com o mesmo header Authorization para baixar o binário.
     * <p>
     * Aqui retornamos a URL para que seu processor/gateway faça o download/autorização.
     * Se preferir baixar aqui, faça um segundo GET com header Authorization e encaminhe o binário ao seu backend.
     */
    private String getMetaMediaUrl(String mediaId, String token) {
        try {
            var http = java.net.http.HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://graph.facebook.com/v20.0/" + mediaId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                // Usa Jackson para extrair o campo "url"
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(resp.body());
                var urlNode = node.get("url");
                if (urlNode != null && !urlNode.isNull()) {
                    return urlNode.asText();
                }
            } else {
                log.warn("Falha ao consultar media-id no Graph. status={}, body={}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.error("Erro ao obter URL do media (Meta): {}", e.getMessage(), e);
        }
        return null;

    }


}
