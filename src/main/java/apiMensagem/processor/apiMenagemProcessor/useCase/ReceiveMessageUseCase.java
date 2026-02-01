package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppWebhookPayload;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;

public interface ReceiveMessageUseCase {

    void receiveMessage(WebhookMessagePayload payload);

    void receiveStatusMessageMeta(WhatsAppWebhookPayload payload);
}
