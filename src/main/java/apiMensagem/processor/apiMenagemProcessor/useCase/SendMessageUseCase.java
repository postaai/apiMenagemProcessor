package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.AudioRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.MessageRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.TypingRequest;

public interface SendMessageUseCase {

    void sendMessageWhatsApp(MessageRequest request);

    void typingMessage(TypingRequest request);

    void sendAudio(AudioRequest request);
}
