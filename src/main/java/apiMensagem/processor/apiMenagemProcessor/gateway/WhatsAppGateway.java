package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppGateway {

    public static final String HOST_URL = "http://92.113.33.84:8080";

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendMessage(String to, String message, String token, String instanceName) throws Exception {
        final RestTemplate restTemplate = new RestTemplate();

        // Monta URL de destino com o nome da instância
        String url = HOST_URL + "/message/sendText/" + instanceName;
        log.info("[ENVIANDO] Enviando mensagem: {}, para {} via {}",message, to, url);

        // Monta headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token); // "apikey" no header, não "Authorization"

        // Monta o corpo da requisição
        Map<String, String> payload = new HashMap<>();
        payload.put("number", to);
        payload.put("text", message);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<WhatsAppResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, WhatsAppResponse.class
            );
            log.info("[SUCESSO] Mensagem enviada para {} - Status: {}", to, response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("[ERRO] Falha ao enviar mensagem para {}: {}", to, e.getMessage(), e);
            return null;
        }
    }

    public void typingMessage(String number, Integer delay, String token, String instanceName) {
        final RestTemplate restTemplate = new RestTemplate();

        // Monta URL com o nome da instância
        String url = HOST_URL + "/chat/sendPresence/" + instanceName;
        log.info("[TYPING] Enviando sinal de 'digitando' para {} via {}", number, url);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        // Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("number", number);
        payload.put("delay", delay);
        payload.put("presence", "composing"); // "composing" = digitando

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("[TYPING] Sinal enviado com sucesso para {} - Status: {}", number, response.getStatusCode());
        } catch (Exception e) {
            log.error("[TYPING][ERRO] Falha ao enviar presença para {}: {}", number, e.getMessage(), e);
        }
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendAudio(String to, String base64Audio, String token, String instanceName) {
        final RestTemplate restTemplate = new RestTemplate();

        // Monta URL de destino
        String url = HOST_URL + "/message/sendWhatsAppAudio/" + instanceName;
        log.info("[AUDIO] Enviando áudio para {} via {}", to, url);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        // Corpo da requisição
        Map<String, Object> payload = new HashMap<>();
        payload.put("number", to);
        payload.put("audio", base64Audio);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("[AUDIO] Áudio enviado com sucesso para {} - Status: {}", to, response.getStatusCode());
        } catch (Exception e) {
            log.error("[AUDIO][ERRO] Falha ao enviar áudio para {}: {}", to, e.getMessage(), e);
        }
    }
}
