package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.MessageRequest;

public interface SendMessageUseCase {

    void sendMessageWhatsApp(MessageRequest request);
}
