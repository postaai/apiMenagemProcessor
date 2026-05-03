package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SendMessageUseCase {

    void sendMessageWhatsApp(MessageRequest request);

    void typingMessage(TypingRequest request);

    void sendAudio(AudioRequest request);

    void sendAudioByMediaId(AudioMediaIdRequest request);

    void sendImageByLink(ImageLinkRequest request);

    UploadMediaResponse uploadMedia(String orgId, MultipartFile file, String mimeType);

    void sendMediaById(SendMediaByIdRequest request);

    void sendLocation(LocationRequest request);

    List<WhatsAppGroupResponse> getWhatsAppGroups(String orgId, boolean participants);

    QrCodePayload generateQRCode(String orgId);

    CheckInstanceResponse checkInstance(String orgId);
}
