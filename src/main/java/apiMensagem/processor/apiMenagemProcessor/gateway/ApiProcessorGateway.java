package apiMensagem.processor.apiMenagemProcessor.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiProcessorGateway {

    @Value("${processor.host}")
    private String hostProcessor;

    public void sendTextMessage(String userId, String orgId, String content, String contactName) {
        RestTemplate restTemplate = new RestTemplate();

        // Criar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Criar body
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("orgId", orgId);
        body.put("contactName", contactName);
        body.put("content", content);

        // Criar request
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hostProcessor + "/recive-text-message", request, String.class);
            log.info("Resposta da API: {}", response.getBody());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage(), e);
        }
    }

    public void sendAudioMessage(String userId, String orgId, String base64Audio, String mimetype, String mediaKey, String contactName){
        RestTemplate restTemplate = new RestTemplate();

        // Criar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Criar body
        Map<String, Object> body = new HashMap<>();
        body.put("base64", base64Audio);
        body.put("mediaKey", mediaKey);
        body.put("mimetype", mimetype);
        body.put("orgId", orgId);
        body.put("userId", userId);
        body.put("contactName", contactName);

        // Criar requestS
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(hostProcessor + "/recive-audio-message", request, String.class);
            log.info("Resposta da API: {}", response.getBody());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage(), e);
        }
    }
}
