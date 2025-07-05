package apiMensagem.processor.apiMenagemProcessor.controller;

import apiMensagem.processor.apiMenagemProcessor.dto.AudioRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.MessageRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.TypingRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import apiMensagem.processor.apiMenagemProcessor.useCase.ReceiveMessageUseCase;
import apiMensagem.processor.apiMenagemProcessor.useCase.SendMessageUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/message")
public class WhatsAppControllerImpl implements WhatsAppController {

    private final SendMessageUseCase sendMessageUseCase;
    private final ReceiveMessageUseCase receiveMessageUseCase;
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
    public ResponseEntity<Void> typing(TypingRequest typingRequest) {

        sendMessageUseCase.typingMessage(typingRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> sendAudio(AudioRequest request) {
        return null;
    }
}
