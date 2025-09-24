package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.MetaWhatsAppWebhookPayload;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;

public interface ReceiveMessageUseCase {

    void receiveMessage(WebhookMessagePayload payload);

    void receiveStatusMessageMeta(MetaWhatsAppWebhookPayload payload);
}
