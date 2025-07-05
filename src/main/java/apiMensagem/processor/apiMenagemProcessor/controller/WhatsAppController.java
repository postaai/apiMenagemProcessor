package apiMensagem.processor.apiMenagemProcessor.controller;

import apiMensagem.processor.apiMenagemProcessor.dto.AudioRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.MessageRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.TypingRequest;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "WhatsApp", description = "WhatsApp Controller")
public interface WhatsAppController {

    @Operation(summary = "sendMensagem")
    @PostMapping("/send-text-message")
    ResponseEntity<Void> sendMessage(@RequestBody @Valid MessageRequest messageRequest);

    @Operation(summary = "Webhook de recebimento de mensagens")
    @PostMapping("/webhook/receive-message")
    ResponseEntity<Void> receiveMessage(@RequestBody WebhookMessagePayload payload);

    @Operation(summary = "typing")
    @PostMapping("/typing")
    ResponseEntity<Void> typing(@RequestBody TypingRequest typingRequest);

    @Operation(summary = "sendAudio")
    @PostMapping("/send-audio")
    ResponseEntity<Void> sendAudio(@RequestBody AudioRequest request);
}
