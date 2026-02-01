package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppWebhookPayload;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import apiMensagem.processor.apiMenagemProcessor.gateway.ApiProcessorGateway;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayImpl;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayMetaImpl;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppMediaGateway;
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
    private final WhatsAppMediaGateway whatsAppMediaGateway;

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
        try {
            log.info("[META][WEBHOOK][IN] payloadRecebido={}", payload);

            if (payload == null || payload.entry == null || payload.entry.isEmpty()) {
                log.error("[META][WEBHOOK][ERRO] payload nulo/vazio: payload={}, entry={}",
                        payload, (payload != null ? payload.entry : null));
                throw new IllegalArgumentException("Payload do webhook está vazio ou nulo");
            }

            var entry = payload.entry.getFirst();
            log.info("[META][WEBHOOK][ENTRY] entryId={} changesCount={}",
                    entry != null ? entry.id : null,
                    (entry != null && entry.changes != null) ? entry.changes.size() : 0);

            if (entry == null || entry.changes == null || entry.changes.isEmpty()) {
                log.info("[META][WEBHOOK][IGNORE] sem changes");
                return;
            }

            var change = entry.changes.getFirst();
            log.info("[META][WEBHOOK][CHANGE] field={} hasValue={} hasContacts={} hasMessages={}",
                    change != null ? change.field : null,
                    change != null && change.value != null,
                    change != null && change.value != null && change.value.contacts != null ? change.value.contacts.size() : 0,
                    change != null && change.value != null && change.value.messages != null ? change.value.messages.size() : 0
            );

            // ✅ CORREÇÃO: ignorar chamadas que não trazem messages (status/keep-alive/updates sem inbound)
            if (change == null || change.value == null || change.value.messages == null || change.value.messages.isEmpty()) {
                log.info("[META][WEBHOOK][IGNORE] webhook sem messages (Meta status/keep-alive)");
                return;
            }

            String idMeta = entry.id;
            if (idMeta == null || idMeta.isBlank()) {
                log.error("[META][WEBHOOK][ERRO] ID Meta não encontrado no entry: entry={}", entry);
                throw new IllegalArgumentException("ID Meta não encontrado no payload");
            }

            log.info("[META][WEBHOOK][RESOLVE_ORG][IN] idMeta={}", idMeta);
            var oroganization = resolveOrganizationByPhoneNumberId(idMeta);
            log.info("[META][WEBHOOK][RESOLVE_ORG][OUT] idMeta={} orgId={}",
                    idMeta, (oroganization != null ? oroganization.orgId() : null));

            if (change.value.contacts == null || change.value.contacts.isEmpty()) {
                log.info("[META][WEBHOOK][INFO] Contacts não encontrado no payload: entryId={}", idMeta);
            }

            String numeroId = null;
            String nomeContato = null;

            if (change.value.contacts != null && !change.value.contacts.isEmpty()) {
                var c0 = change.value.contacts.getFirst();
                numeroId = c0.waId;
                nomeContato = (c0.profile != null ? c0.profile.name : null);
                log.info("[META][WEBHOOK][CONTACT] waId={} nome={}", numeroId, nomeContato);
            }

            var m0 = change.value.messages.getFirst();
            String tipo = m0.type;
            log.info("[META][WEBHOOK][MESSAGE] messageId={} from={} type={} timestamp={}",
                    m0.id, m0.from, tipo, m0.timestamp);

            switch (tipo) {

                case "text": {
                    String textoRecebido = (m0.text != null ? m0.text.body : null);

                    log.info("[META][WEBHOOK][TEXT][IN] waId={} orgId={} nome={}",
                            numeroId,
                            (oroganization != null ? oroganization.orgId() : null),
                            nomeContato);

                    apiProcessorGateway.sendTextMessage(
                            numeroId,
                            oroganization.orgId(),
                            textoRecebido,
                            nomeContato
                    );

                    log.info("[META][WEBHOOK][TEXT][OK] waId={} messageId={}", numeroId, m0.id);
                    break;
                }

                case "audio": {
                    String messageId = m0.id;
                    String mediaId = (m0.audio != null ? m0.audio.id : null);

                    log.info("[META][AUDIO][IN] waId={} messageId={} mediaId={}", numeroId, messageId, mediaId);

                    if (mediaId == null || mediaId.isBlank()) {
                        log.error("[META][AUDIO][ERRO] mediaId não encontrado: messageId={}", messageId);
                        break;
                    }

                    assert oroganization != null;
                    var mediaMeta = whatsAppMediaGateway.getMediaMeta(oroganization);

                    if (mediaMeta == null || mediaMeta.url() == null || mediaMeta.url().isBlank()) {
                        log.error("[META][AUDIO][ERRO] metadata inválida: mediaId={}", mediaId);
                        break;
                    }

                    byte[] audioBytes = whatsAppMediaGateway.downloadMediaBinary(oroganization, mediaMeta.url());

                    if (audioBytes == null || audioBytes.length == 0) {
                        log.error("[META][AUDIO][ERRO] áudio vazio: mediaId={}", mediaId);
                        break;
                    }

                    apiProcessorGateway.sendAudioMessage(
                            numeroId,
                            oroganization.orgId(),
                            mediaMeta.url(),
                            mediaMeta.mimeType(),
                            "mediaKeyPlaceholder",
                            nomeContato
                    );

                    log.info("[META][AUDIO][OK] waId={} messageId={}", numeroId, messageId);
                    break;
                }

                case "image":
                    log.info("[META][WEBHOOK][IMAGE] messageId={} from={}", m0.id, m0.from);
                    break;

                case "video":
                    log.info("[META][WEBHOOK][VIDEO] messageId={} from={}", m0.id, m0.from);
                    break;

                case "document":
                    log.info("[META][WEBHOOK][DOCUMENT] messageId={} from={}", m0.id, m0.from);
                    break;

                case "location":
                    log.info("[META][WEBHOOK][LOCATION] messageId={} from={}", m0.id, m0.from);
                    break;

                default:
                    log.info("[META][WEBHOOK][UNHANDLED] type={} messageId={} from={}", tipo, m0.id, m0.from);
                    break;
            }

        } catch (Exception e) {
            log.error("[META][WEBHOOK][EXCEPTION] erro ao processar webhook: payload={}", payload, e);
            throw e;
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
