package apiMensagem.processor.apiMenagemProcessor.controller;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
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
            log.info("📥 Mensagem recebida via webhook:\n{}", json);
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter payload para JSON: {}", e.getMessage());
        }

        receiveMessageUseCase.receiveMessage(payload);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<String> verifyMetaWebhook(String mode, String token, String challenge) {
        if ("subscribe".equalsIgnoreCase(mode) && token != null) {
            var org = organizationRepository.findByTokenWebhookMeta(token);
            if (org.isPresent()) {
                if (Boolean.FALSE.equals(org.get().ativo())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Organização inativa");
                }
                return ResponseEntity.ok(challenge == null ? "" : challenge);
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
    }

    @Override
    public ResponseEntity<Void> receiveMessageMeta(WhatsAppWebhookPayload payload) {
        log.info("📥 Mensagem recebida via webhook Meta:\n{}", payload);
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
