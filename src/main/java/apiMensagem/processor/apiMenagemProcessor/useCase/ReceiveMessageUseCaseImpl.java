package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import apiMensagem.processor.apiMenagemProcessor.gateway.ApiProcessorGateway;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveMessageUseCaseImpl implements ReceiveMessageUseCase {

    private final OrganizationRepository organizationRepository;
    private final ApiProcessorGateway apiProcessorGateway;

    @Override
    public void receiveMessage(WebhookMessagePayload payload) {
        try {
            if (payload == null || payload.getData() == null) {
                log.warn("Payload inválido ou nulo.");
                return;
            }

            String messageType = payload.getData().getMessageType();

            if (!"conversation".equalsIgnoreCase(messageType)) {
                log.info("Mensagem ignorada. Tipo de mensagem não suportado: {}", messageType);
                return;
            }

            String remoteJid = payload.getData().getKey() != null ? payload.getData().getKey().getRemoteJid() : null;
            String conversation = payload.getData().getMessage().getConversation();
            String orgId = payload.getInstance();

            if (remoteJid == null || conversation == null || orgId == null) {
                log.warn("Dados incompletos no payload.");
                return;
            }

            remoteJid = remoteJid.replace("@s.whatsapp.net", "");

            Optional<OrganizationsEntity> organizationOpt = organizationRepository.findByorgId(orgId);
            if (organizationOpt.isEmpty()) {
                log.warn("Organização com orgId [{}] não encontrada", orgId);
                return;
            }

            apiProcessorGateway.sendTextMessage(remoteJid, orgId, conversation);
            log.info("Mensagem de texto processada: [{}] - [{}]", remoteJid, conversation);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida: {}", e.getMessage(), e);
        }
    }
}
