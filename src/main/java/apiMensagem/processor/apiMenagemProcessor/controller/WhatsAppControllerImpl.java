package apiMensagem.processor.apiMenagemProcessor.controller;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import org.springframework.web.multipart.MultipartFile;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import apiMensagem.processor.apiMenagemProcessor.useCase.ReceiveMessageUseCase;
import apiMensagem.processor.apiMenagemProcessor.useCase.SendMessageUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/message")
public class WhatsAppControllerImpl implements WhatsAppController {

    private final SendMessageUseCase sendMessageUseCase;
    private final ReceiveMessageUseCase receiveMessageUseCase;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;


    @Override
    public ResponseEntity<Void> sendMessage(MessageRequest messageRequest) {
        sendMessageUseCase.sendMessageWhatsApp(messageRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> receiveMessage(WebhookMessagePayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            log.info("[WEBHOOK][RECEIVE][IN] payload={}", json);
        } catch (JsonProcessingException e) {
            log.error("[WEBHOOK][RECEIVE][ERRO] falha ao converter payload para JSON: {}", e.getMessage());
        }

        receiveMessageUseCase.receiveMessage(payload);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<String> verifyMetaWebhook(String mode, String token, String challenge) {
        log.info("[META][WEBHOOK][VERIFY][IN] mode={} token={} challenge={}", mode, token, challenge);
        if ("subscribe".equalsIgnoreCase(mode) && token != null) {
            var org = organizationRepository.findByTokenWebhookMeta(token);
            if (org.isPresent()) {
                if (Boolean.FALSE.equals(org.get().ativo())) {
                    log.warn("[META][WEBHOOK][VERIFY][REJECTED] organizacao inativa token={}", token);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Organização inativa");
                }
                log.info("[META][WEBHOOK][VERIFY][OK] token={} challenge={}", token, challenge);
                return ResponseEntity.ok(challenge == null ? "" : challenge);
            }
        }
        log.warn("[META][WEBHOOK][VERIFY][REJECTED] token invalido ou mode diferente de subscribe mode={} token={}", mode, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
    }

    @Override
    public ResponseEntity<Void> receiveMessageMeta(WhatsAppWebhookPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            log.info("[META][WEBHOOK][RECEIVE][IN] payload={}", json);
        } catch (JsonProcessingException e) {
            log.error("[META][WEBHOOK][RECEIVE][ERRO] falha ao converter payload para JSON: {}", e.getMessage());
        }

        receiveMessageUseCase.receiveStatusMessageMeta(payload);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> typing(TypingRequest typingRequest) {

        sendMessageUseCase.typingMessage(typingRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendAudio(AudioRequest request) {
        sendMessageUseCase.sendAudio(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendAudioByMediaId(AudioMediaIdRequest request) {
        sendMessageUseCase.sendAudioByMediaId(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendImageByLink(ImageLinkRequest request) {
        sendMessageUseCase.sendImageByLink(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<UploadMediaResponse> uploadMedia(String orgId, MultipartFile file, String type) {
        var response = sendMessageUseCase.uploadMedia(orgId, file, type);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> sendMediaById(SendMediaByIdRequest request) {
        sendMessageUseCase.sendMediaById(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendMediaByLink(MediaLinkRequest request) {
        sendMessageUseCase.sendMediaByLink(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<WhatsAppGroupResponse>> findGroups(String orgId, boolean participants) {
       var groups = sendMessageUseCase.getWhatsAppGroups(orgId, participants);
        return ResponseEntity.ok(groups);
    }

    @Override
    public ResponseEntity<Void> sendLocation(LocationRequest request) {
        sendMessageUseCase.sendLocation(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<QrCodePayload> generateQRCode(String orgId) {
        var qrCode = sendMessageUseCase.generateQRCode(orgId);
        return ResponseEntity.ok(qrCode);
    }

    @Override
    public ResponseEntity<CheckInstanceResponse> checkConnection(String orgId) {
        var response = sendMessageUseCase.checkInstance(orgId);
        return ResponseEntity.ok(response);
    }
}
