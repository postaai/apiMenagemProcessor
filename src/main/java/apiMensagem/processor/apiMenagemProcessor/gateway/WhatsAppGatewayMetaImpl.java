package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppGroupResponse;
import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppGatewayMetaImpl {

    // Versão do Graph API (ajuste conforme seu provisionamento)
    private static final String API_VERSION = "v20.0"; // ou v21.0/v23.0 se estiver liberado
    private static final String GRAPH_BASE = "https://graph.facebook.com/" + API_VERSION;
    private final WebClient webClient;

    public WhatsAppGatewayMetaImpl() {
        this.webClient = WebClient.builder()
                .baseUrl("https://graph.facebook.com") // base da Graph API
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendMessage(String to, String message, String token, String phoneNumberId, String numberIdMeta) {
        // Monta o JSON conforme o requerido pela API da Meta
        log.info("teste sendMessage meta to {} message {} token {} phoneNumberId {} numberIdMeta {}", to, message, token, phoneNumberId, numberIdMeta);
        var body = Map.<String, Object>of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", message)
        );

        log.info("teste body meta {}", body);
        // Faz a chamada HTTP
        // A URL segue: /{version}/{phoneNumberId}/messages
        // Ex.: /v21.0/966475936553995/messages
        return webClient.post()
                .uri("/v22.0/" + numberIdMeta + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token) // Bearer token enviado
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WhatsAppResponse.class)
                .block(); // Pode bloquear aqui ou retornar um Mono, conforme sua preferência

    }

    public void typingMessage(String number, Integer delaySeconds, String token, String phoneNumberId) {

    }

    /**
     * Envio de áudio:
     * - Cloud API aceita “link” PÚBLICO ou “id” de mídia previamente enviada em /{PHONE_NUMBER_ID}/media.
     * - Base64 NÃO é aceito diretamente. Aqui, se vier um http(s) usaremos como link; caso contrário, faremos upload e usaremos o id. :contentReference[oaicite:4]{index=4}
     */
    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendAudio(String to, String base64Audio, String token, String phoneNumberId, String numberIdMeta) {

        // 1) Decodifica base64 (aceita também "data:audio/ogg;base64,....")
        String mimeType = "audio/ogg"; // default seguro para voice note (ogg/opus)
        String b64 = base64Audio;

        if (base64Audio != null && base64Audio.startsWith("data:")) {
            // data:<mime>;base64,<data>
            int comma = base64Audio.indexOf(',');
            if (comma > 0) {
                String header = base64Audio.substring(5, comma); // remove "data:"
                int semi = header.indexOf(';');
                if (semi > 0) mimeType = header.substring(0, semi);
                b64 = base64Audio.substring(comma + 1);
            }
        }

        byte[] audioBytes = Base64.getDecoder().decode(b64);

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        // 2) Upload da mídia -> retorna mediaId
        // POST /vXX.X/{phone-number-id}/media  (multipart/form-data)
        // Campos: messaging_product=whatsapp, file, type=<mime>
        // Doc: Media upload (Cloud API). :contentReference[oaicite:0]{index=0}
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("messaging_product", "whatsapp");
        mb.part("type", mimeType);

        mb.part("file", new ByteArrayResource(audioBytes) {
                    @Override public String getFilename() { return "audio"; }
                })
                .header(HttpHeaders.CONTENT_TYPE, mimeType);

        log.info("token meta {}", token);

        String mediaId = graph.post()
                .uri("/v22.0/{phoneNumberId}/media", numberIdMeta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Meta media upload erro: " + body)))
                )
                .bodyToMono(MediaUploadResponse.class)
                .map(MediaUploadResponse::id)
                .block();

        if (mediaId == null || mediaId.isBlank()) {
            throw new IllegalStateException("Meta não retornou mediaId no upload do áudio");
        }

        // 3) Envia a mensagem de áudio referenciando o mediaId
        // POST /vXX.X/{phone-number-id}/messages com type="audio" e audio.id=<mediaId>
        // Doc: Audio messages / Message API. :contentReference[oaicite:1]{index=1}
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "audio",
                "audio", Map.of("id", mediaId)
        );

        graph.post()
                .uri("/v22.0/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Meta send audio erro: " + body)))
                )
                .bodyToMono(String.class)
                .block();
    }

    private record MediaUploadResponse(String id) {}

    /**
     * Envio de localização:
     * POST /{PHONE_NUMBER_ID}/messages com type=location e {latitude, longitude, name, address}. :contentReference[oaicite:5]{index=5}
     */
    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendLocation(String to, String name, String address, double latitude, double longitude, String token, String phoneNumberId) {
        return WhatsAppResponse.builder().build();
    }

    /**
     * Grupos: NÃO suportado na Cloud API oficial (envio é 1:1 / templates / interativos; grupos demandam soluções não-oficiais).
     * Lançamos UnsupportedOperationException. :contentReference[oaicite:6]{index=6}
     */

    public List<WhatsAppGroupResponse> fetchAllGroups(String token, String instanceName, boolean getParticipants) {
        log.error("[META][GROUPS] Não suportado pela WhatsApp Cloud API oficial.");
        throw new UnsupportedOperationException("WhatsApp Cloud API não expõe endpoints de grupos.");
    }

    // === RECOVERs ===
    @Recover
    public WhatsAppResponse recoverSendMessage(Exception e, String to, String message, String token, String phoneNumberId) {
        log.error("[META][RECOVER][TEXT] to={} erro={}", to, e.toString(), e);
        return null;
    }

    @Recover
    public void recoverSendAudio(Exception e, String to, String base64Audio, String token, String phoneNumberId) {
        log.error("[META][RECOVER][AUDIO] to={} erro={}", to, e.toString(), e);
    }
}
