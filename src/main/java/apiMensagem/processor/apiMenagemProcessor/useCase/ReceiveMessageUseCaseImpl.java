package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppWebhookPayload;
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
                    log.info("Mensagem de texto enviada: [{}] - [{}] - [{}]", remoteJid, conversation, contactName);
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
    public void receiveStatusMessageMeta(WhatsAppWebhookPayload payload) {
        if (payload == null || payload.entry == null || payload.entry.isEmpty()) {
            throw new IllegalArgumentException("Payload do webhook está vazio ou nulo");
        }

        var entry = payload.entry.getFirst();

        String idMeta = entry.id;
        if (idMeta == null || idMeta.isBlank()) {
            throw new IllegalArgumentException("ID Meta não encontrado no payload");
        }

        var oroganization = resolveOrganizationByPhoneNumberId(idMeta);

        if (entry.changes == null || entry.changes.isEmpty()) {
            throw new IllegalArgumentException("Changes não encontrado no payload");
        }

        var change = entry.changes.getFirst();

        if (change.value == null || change.value.contacts == null || change.value.contacts.isEmpty()) {
            throw new IllegalArgumentException("Contacts não encontrado no payload");
        }

        String numeroId = change.value.contacts.getFirst().waId;

        if (change.value.messages == null || change.value.messages.isEmpty()) {
            throw new IllegalArgumentException("Messages não encontrado no payload");
        }

        String tipo = change.value.messages.getFirst().type;

        switch (tipo) {

            case "text":
                String textoRecebido = change.value.messages.get(0).text.body;
                String nomeContato = change.value.contacts.get(0).profile.name;

                apiProcessorGateway.sendTextMessage(
                        numeroId,          // numero que enviou a mensagem
                        oroganization.orgId(),
                        textoRecebido,     // texto recebido no webhook
                        nomeContato        // nome de quem enviou a mensagem
                );

            case "audio":
                break;

            case "image":
                break;

            case "video":
                break;

            case "document":
                break;

            case "location":
                break;

            default:
                break;
        }


    }


    private OrganizationsEntity resolveOrganizationByPhoneNumberId(String idMeta) {
        // Ajuste conforme seu repository. Ex.: findByPhoneNumberId, findByInstanceName, etc.
        // Se seu domínio usa orgId como 'instance', mapeie phoneNumberId->orgId numa tabela.
        return organizationRepository.findByIdMeta(idMeta).orElse(null);
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
