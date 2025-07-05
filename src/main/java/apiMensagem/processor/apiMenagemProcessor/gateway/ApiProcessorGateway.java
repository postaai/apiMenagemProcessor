package apiMensagem.processor.apiMenagemProcessor.gateway;

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
public class ApiProcessorGateway {

    private static final String API_URL = "https://4185-2803-9810-4638-2810-3000-cd2c-ac08-872a.ngrok-free.app/recive-text-message";

    public void sendTextMessage(String userId, String orgId, String content) {
        RestTemplate restTemplate = new RestTemplate();

        // Criar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Criar body
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("orgId", orgId);
        body.put("content", content);

        // Criar request
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
            log.info("Resposta da API: {}", response.getBody());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage(), e);
        }
    }
}
