package apiMensagem.processor.apiMenagemProcessor.controller;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import apiMensagem.processor.apiMenagemProcessor.dto.messagePayload.WebhookMessagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WhatsApp", description = "WhatsApp Controller")
public interface WhatsAppController {

    @Operation(summary = "sendMensagem")
    @PostMapping("/send-text-message")
    ResponseEntity<Void> sendMessage(@RequestBody @Valid MessageRequest messageRequest);

    @Operation(summary = "Webhook de recebimento de mensagens")
    @PostMapping("/webhook/receive-message")
    ResponseEntity<Void> receiveMessage(@RequestBody WebhookMessagePayload payload);

    @Operation(summary = "Webhook de recebimento de mensagens meta")
    @PostMapping("/webhook/receive-message/meta")
    ResponseEntity<Void> receiveMessageMeta(@RequestBody MetaWhatsAppWebhookPayload payload);

    @GetMapping(value = "/webhook/receive-message/meta", produces = MediaType.TEXT_PLAIN_VALUE)
     ResponseEntity<String> verifyMetaWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge);

    @Operation(summary = "typing")
    @PostMapping("/typing")
    ResponseEntity<Void> typing(@RequestBody TypingRequest typingRequest);

    @Operation(summary = "sendAudio")
    @PostMapping("/send-audio")
    ResponseEntity<Void> sendAudio(@RequestBody AudioRequest request);

    @Operation(summary = "findGroups")
    @PostMapping("/find-groups")
    ResponseEntity<List<WhatsAppGroupResponse>> findGroups(@Parameter(description = "ID da organização") String orgId, @Parameter(description = "Incluir participantes") boolean participants);

    @Operation(summary = "sendLocation")
    @PostMapping("/send-location")
    ResponseEntity<Void> sendLocation(@RequestBody LocationRequest request);

    @Operation(summary = "generateQRCode")
    @GetMapping("/generate-qrcode/{orgId}")
    ResponseEntity<QrCodePayload> generateQRCode(@Parameter(description = "ID da organização") @PathVariable String orgId);

    @Operation(summary = "checkConnection" )
    @GetMapping("/check-connection/{orgId}")
    ResponseEntity<CheckInstanceResponse> checkConnection(@Parameter(description = "ID da organização") @PathVariable String orgId);
}
