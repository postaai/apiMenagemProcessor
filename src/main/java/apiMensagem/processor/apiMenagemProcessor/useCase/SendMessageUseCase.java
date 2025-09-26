package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.*;

import java.util.List;

public interface SendMessageUseCase {

    void sendMessageWhatsApp(MessageRequest request);

    void typingMessage(TypingRequest request);

    void sendAudio(AudioRequest request);

    void sendLocation(LocationRequest request);

    List<WhatsAppGroupResponse> getWhatsAppGroups(String orgId, boolean participants);

    QrCodePayload generateQRCode(String orgId);

    CheckInstanceResponse checkInstance(String orgId);
}
