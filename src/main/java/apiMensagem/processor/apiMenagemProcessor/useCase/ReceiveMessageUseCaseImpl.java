package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import apiMensagem.processor.apiMenagemProcessor.gateway.ApiProcessorGateway;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGateway;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystemNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveMessageUseCaseImpl implements ReceiveMessageUseCase {

    private final OrganizationRepository organizationRepository;
    private final ApiProcessorGateway apiProcessorGateway;
    private final WhatsAppGateway whatsAppGateway;

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
            String instanceName = org.instanceName(); // ajuste o nome do campo conforme necessário

            switch (messageType) {
                case "conversation":
                    String conversation = payload.getData().getMessage().getConversation();
                    apiProcessorGateway.sendTextMessage(remoteJid, orgId, conversation);
                    log.info("Mensagem de texto enviada: [{}] - [{}]", remoteJid, conversation);
                    break;

                case "audioMessage":
                    String audioUrl = payload.getData().getMessage().getAudioMessage().getUrl();
                    var audioMessage = payload.getData().getMessage().getAudioMessage();
                    if (payload.getData().getMessage().getAudioMessage().getSeconds() < limitAudio) {
                        if (audioUrl != null) {
                            apiProcessorGateway.sendAudioMessage(remoteJid, orgId, audioMessage.getUrl(), audioMessage.getMimetype(), audioMessage.getMediaKey());
                            log.info("Áudio enviado: [{}] - [{}]", remoteJid, audioUrl);
                        } else {
                            log.warn("URL do áudio ausente.");
                        }
                    }else {
                        String mensagemPadrao = "Recebemos seu áudio! 😊 Para conseguirmos te ajudar melhor, envie áudios com até " + limitAudio + " segundos.";
                        whatsAppGateway.sendMessage(remoteJid, mensagemPadrao, token, instanceName);
                    }
                    break;

                default:
                    String mensagemPadrao = "Desculpe, ainda não entendi esse tipo de mensagem. No momento só aceitamos mensagens de texto ou áudio.";
                    whatsAppGateway.sendMessage(remoteJid, mensagemPadrao, token, instanceName);
                    log.info("Mensagem padrão enviada para [{}] - [{}]", remoteJid, mensagemPadrao);
                    break;
            }

        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
        }
    }
}
