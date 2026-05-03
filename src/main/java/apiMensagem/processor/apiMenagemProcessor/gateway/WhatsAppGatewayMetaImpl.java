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

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.HashMap;
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

        log.info("[META][SEND_AUDIO][IN] to={} phoneNumberId={} numberIdMeta={}",
                to, phoneNumberId, numberIdMeta);

        if (base64Audio == null || base64Audio.isBlank()) {
            log.error("[META][SEND_AUDIO][ERRO] base64Audio vazio");
            throw new IllegalArgumentException("base64Audio vazio");
        }

        // 🔐 Log seguro do base64
        log.info("[META][SEND_AUDIO][BASE64] length={} previewStart={} previewEnd={}",
                base64Audio.length(),
                base64Audio.substring(0, Math.min(60, base64Audio.length())),
                base64Audio.substring(Math.max(0, base64Audio.length() - 60))
        );

        // 1) Decodifica base64
        String mimeType = "audio/ogg";
        String b64 = base64Audio;

        if (base64Audio.startsWith("data:")) {
            int comma = base64Audio.indexOf(',');
            if (comma > 0) {
                String header = base64Audio.substring(5, comma);
                int semi = header.indexOf(';');
                if (semi > 0) {
                    mimeType = header.substring(0, semi);
                }
                b64 = base64Audio.substring(comma + 1);
            }
        }

        log.info("[META][SEND_AUDIO][DECODE] mimeType={}", mimeType);

        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            log.error("[META][SEND_AUDIO][ERRO] falha ao decodificar base64", e);
            throw e;
        }

        log.info("[META][SEND_AUDIO][BYTES] bytesLength={}", audioBytes.length);

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        // 2) Upload da mídia
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("messaging_product", "whatsapp");
        mb.part("type", mimeType);

        mb.part("file", new ByteArrayResource(audioBytes) {
                    @Override
                    public String getFilename() {
                        return "audio.ogg";
                    }
                })
                .header(HttpHeaders.CONTENT_TYPE, mimeType);

        log.info("[META][SEND_AUDIO][UPLOAD][IN] endpoint=/v22.0/{}/media", numberIdMeta);

        String mediaId = graph.post()
                .uri("/v22.0/{phoneNumberId}/media", numberIdMeta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][SEND_AUDIO][UPLOAD][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta media upload erro: " + body));
                        })
                )
                .bodyToMono(MediaUploadResponse.class)
                .map(MediaUploadResponse::id)
                .block();

        log.info("[META][SEND_AUDIO][UPLOAD][OUT] mediaId={}", mediaId);

        if (mediaId == null || mediaId.isBlank()) {
            log.error("[META][SEND_AUDIO][ERRO] mediaId vazio após upload");
            throw new IllegalStateException("Meta não retornou mediaId no upload do áudio");
        }

        // 3) Envio da mensagem de áudio
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "audio",
                "audio", Map.of("id", mediaId)
        );

        log.info("[META][SEND_AUDIO][MESSAGE][IN] endpoint=/v22.0/{}/messages payload={}",
                phoneNumberId, payload);

        graph.post()
                .uri("/v22.0/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][SEND_AUDIO][MESSAGE][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta send audio erro: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("[META][SEND_AUDIO][MESSAGE][OK] response={}", resp))
                .block();

        log.info("[META][SEND_AUDIO][DONE] to={} mediaId={}", to, mediaId);
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendAudioByMediaId(String to, String mediaId, String token, String phoneNumberId) {

        log.info("[META][SEND_AUDIO_MEDIA_ID][IN] to={} mediaId={} phoneNumberId={}", to, mediaId, phoneNumberId);

        if (mediaId == null || mediaId.isBlank()) {
            log.error("[META][SEND_AUDIO_MEDIA_ID][ERRO] mediaId vazio");
            throw new IllegalArgumentException("mediaId vazio");
        }

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "audio",
                "audio", Map.of("id", mediaId)
        );

        log.info("[META][SEND_AUDIO_MEDIA_ID][MESSAGE][IN] endpoint=/v22.0/{}/messages payload={}", phoneNumberId, payload);

        graph.post()
                .uri("/v22.0/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][SEND_AUDIO_MEDIA_ID][MESSAGE][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta send audio by mediaId erro: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("[META][SEND_AUDIO_MEDIA_ID][MESSAGE][OK] response={}", resp))
                .block();

        log.info("[META][SEND_AUDIO_MEDIA_ID][DONE] to={} mediaId={}", to, mediaId);
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

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendImageByLink(String to, String link, String caption, String token, String phoneNumberId) {

        log.info("[META][SEND_IMAGE_LINK][IN] to={} phoneNumberId={} link={}", to, phoneNumberId, link);

        if (link == null || link.isBlank()) {
            throw new IllegalArgumentException("link da imagem vazio");
        }

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        Map<String, Object> imagePayload = new java.util.HashMap<>();
        imagePayload.put("link", link);
        if (caption != null && !caption.isBlank()) {
            imagePayload.put("caption", caption);
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "image",
                "image", imagePayload
        );

        log.info("[META][SEND_IMAGE_LINK][MESSAGE][IN] endpoint=/v22.0/{}/messages payload={}", phoneNumberId, payload);

        graph.post()
                .uri("/v22.0/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][SEND_IMAGE_LINK][MESSAGE][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta send image by link erro: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("[META][SEND_IMAGE_LINK][MESSAGE][OK] response={}", resp))
                .block();

        log.info("[META][SEND_IMAGE_LINK][DONE] to={} link={}", to, link);
    }

    @Recover
    public void recoverSendImageByLink(Exception e, String to, String link, String caption, String token, String phoneNumberId) {
        log.error("[META][RECOVER][IMAGE_LINK] to={} link={} erro={}", to, link, e.toString(), e);
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String uploadMedia(MultipartFile file, String mimeType, String token, String phoneNumberId) {
        log.info("[META][UPLOAD_MEDIA][IN] phoneNumberId={} mimeType={} fileName={} size={}",
                phoneNumberId, mimeType, file.getOriginalFilename(), file.getSize());

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("[META][UPLOAD_MEDIA][ERRO] Falha ao ler bytes do arquivo", e);
            throw new RuntimeException("Falha ao ler arquivo para upload", e);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "media";

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("messaging_product", "whatsapp");
        mb.part("type", mimeType);
        mb.part("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).header(HttpHeaders.CONTENT_TYPE, mimeType);

        String mediaId = graph.post()
                .uri("/v22.0/{phoneNumberId}/media", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][UPLOAD_MEDIA][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta media upload erro: " + body));
                        })
                )
                .bodyToMono(MediaUploadResponse.class)
                .map(MediaUploadResponse::id)
                .block();

        if (mediaId == null || mediaId.isBlank()) {
            throw new IllegalStateException("Meta não retornou mediaId no upload");
        }

        log.info("[META][UPLOAD_MEDIA][DONE] phoneNumberId={} mediaId={}", phoneNumberId, mediaId);
        return mediaId;
    }

    @Recover
    public String recoverUploadMedia(Exception e, MultipartFile file, String mimeType, String token, String phoneNumberId) {
        log.error("[META][RECOVER][UPLOAD_MEDIA] phoneNumberId={} mimeType={} erro={}", phoneNumberId, mimeType, e.toString(), e);
        throw new RuntimeException("Falha ao fazer upload de mídia após tentativas: " + e.getMessage(), e);
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendMediaById(String to, String mediaId, String type, String caption, String token, String phoneNumberId) {
        log.info("[META][SEND_MEDIA_ID][IN] to={} type={} mediaId={} phoneNumberId={}", to, type, mediaId, phoneNumberId);

        if (mediaId == null || mediaId.isBlank()) {
            throw new IllegalArgumentException("mediaId vazio");
        }

        WebClient graph = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        Map<String, Object> mediaPayload = new HashMap<>();
        mediaPayload.put("id", mediaId);
        if (caption != null && !caption.isBlank() && !"audio".equalsIgnoreCase(type)) {
            mediaPayload.put("caption", caption);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", type.toLowerCase());
        payload.put(type.toLowerCase(), mediaPayload);

        log.info("[META][SEND_MEDIA_ID][MESSAGE][IN] endpoint=/v22.0/{}/messages payload={}", phoneNumberId, payload);

        graph.post()
                .uri("/v22.0/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][SEND_MEDIA_ID][MESSAGE][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta send media by id erro: " + body));
                        })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("[META][SEND_MEDIA_ID][MESSAGE][OK] response={}", resp))
                .block();

        log.info("[META][SEND_MEDIA_ID][DONE] to={} type={} mediaId={}", to, type, mediaId);
    }

    @Recover
    public void recoverSendMediaById(Exception e, String to, String mediaId, String type, String caption, String token, String phoneNumberId) {
        log.error("[META][RECOVER][SEND_MEDIA_ID] to={} type={} mediaId={} erro={}", to, type, mediaId, e.toString(), e);
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

    @Recover
    public void recoverSendAudioByMediaId(Exception e, String to, String mediaId, String token, String phoneNumberId) {
        log.error("[META][RECOVER][AUDIO_MEDIA_ID] to={} mediaId={} erro={}", to, mediaId, e.toString(), e);
    }
}
