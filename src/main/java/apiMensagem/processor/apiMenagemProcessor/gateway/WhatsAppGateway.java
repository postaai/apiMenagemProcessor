package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.dto.WhatsAppResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppGateway {

    public WhatsAppResponse sendMessage(String to, String message, String token, String instanceName) throws Exception {
        final RestTemplate restTemplate = new RestTemplate();


        // Monta URL de destino com o nome da instância
        String url = "http://92.113.33.84:8080/message/sendText/" + instanceName;
        log.info("[ENVIANDO] Enviando mensagem para {} via {}", to, url);

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
}
