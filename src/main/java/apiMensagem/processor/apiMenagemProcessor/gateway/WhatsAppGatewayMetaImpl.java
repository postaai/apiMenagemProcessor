package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppGroupResponse;
import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

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

    private HttpHeaders authJson(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authMultipart(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private record WaSendText(String messaging_product, String to, String type, Map<String, Object> text) {
    }

    private record WaSendLocation(String messaging_product, String to, String type, Map<String, Object> location) {
    }

    private record WaSendAudioLink(String messaging_product, String to, String type, Map<String, Object> audio) {
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendMessage(String to, String message, String token, String phoneNumberId, String numberIdMeta) {
        final RestTemplate rt = new RestTemplate();
        final String url = GRAPH_BASE + "/" + numberIdMeta + "/messages";

        log.info("[META][TEXT] to={} url={}", to, url);

        var payload = new WaSendText(
                "whatsapp",
                to,
                "text",
                Map.of("preview_url", true, "body", message) // preview_url conforme doc
        ); // enviar texto: /{PHONE_NUMBER_ID}/messages type=text. :contentReference[oaicite:1]{index=1}

        ResponseEntity<Map<String, Object>> resp = rt.exchange(url, HttpMethod.POST,
                new HttpEntity<>(payload, authJson(token)), (Class<Map<String, Object>>) (Class<?>) Map.class);

        log.info("[META][TEXT][OK] status={} body={}", resp.getStatusCode(), resp.getBody());

        WhatsAppResponse out = new WhatsAppResponse();
        Object id = null;
        Map<String, Object> body = resp.getBody();
        if (body != null && body.get("messages") instanceof List<?> messagesList && !messagesList.isEmpty()) {
            Object firstMsg = messagesList.get(0);
            if (firstMsg instanceof Map<?, ?> msgMap && msgMap.get("id") != null) {
                id = msgMap.get("id");
            }
        }
        out.setId(id != null ? id.toString() : null);
        out.setStatus(resp.getStatusCode().toString());
        return out;
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
        final RestTemplate rt = new RestTemplate();
        final String urlMsg = GRAPH_BASE + "/" + numberIdMeta + "/messages";

        String audioIdOrLink;
        boolean isLink = base64Audio.startsWith("http://") || base64Audio.startsWith("https://");

        if (isLink) {
            audioIdOrLink = base64Audio; // usar como link direto
        } else {
            // upload multipart para obter media-id
            final String urlUpload = GRAPH_BASE + "/" + numberIdMeta + "/media";
            log.info("[META][AUDIO][UPLOAD] url={}", urlUpload);

            byte[] bytes = Base64.getDecoder().decode(
                    base64Audio.contains(",") ? base64Audio.substring(base64Audio.indexOf(',') + 1) : base64Audio
            );

            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return "audio.ogg";
                }
            };

            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", resource);
            form.add("type", "audio/ogg"); // ajuste se necessário
            form.add("messaging_product", "whatsapp");

            ResponseEntity<Map> up = rt.exchange(urlUpload, HttpMethod.POST,
                    new HttpEntity<>(form, authMultipart(token)), Map.class);

            // resposta contém "id" (media id)
            audioIdOrLink = up.getBody() != null ? String.valueOf(up.getBody().get("id")) : null;
            log.info("[META][AUDIO][UPLOAD][OK] media_id={}", audioIdOrLink);
        }

        log.info("[META][AUDIO][SEND] to={} via {}", to, urlMsg);

        Map<String, Object> audioPayload = isLink
                ? Map.of("link", audioIdOrLink)
                : Map.of("id", audioIdOrLink);

        var payload = new WaSendAudioLink("whatsapp", to, "audio", audioPayload);

        ResponseEntity<Map> resp = rt.exchange(urlMsg, HttpMethod.POST,
                new HttpEntity<>(payload, authJson(token)), Map.class);
        log.info("[META][AUDIO][OK] status={} body={}", resp.getStatusCode(), resp.getBody());
    }

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
        final RestTemplate rt = new RestTemplate();
        final String url = GRAPH_BASE + "/" + phoneNumberId + "/messages";

        log.info("[META][LOCATION] to={} lat={},lng={} url={}", to, latitude, longitude, url);

        var payload = new WaSendLocation(
                "whatsapp",
                to,
                "location",
                Map.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "name", (name == null || name.isBlank()) ? "Localização" : name,
                        "address", (address == null ? "" : address)
                )
        );

        ResponseEntity<Map> resp = rt.exchange(url, HttpMethod.POST,
                new HttpEntity<>(payload, authJson(token)), Map.class);

        log.info("[META][LOCATION][OK] status={} body={}", resp.getStatusCode(), resp.getBody());

        WhatsAppResponse out = new WhatsAppResponse();
        Object id = ((Map) ((List) resp.getBody().get("messages")).get(0)).get("id");
        out.setId(id != null ? id.toString() : null);
        out.setStatus(resp.getStatusCode().toString());
        return out;
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
