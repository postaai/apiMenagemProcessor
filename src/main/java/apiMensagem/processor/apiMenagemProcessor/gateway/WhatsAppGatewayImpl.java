package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppGatewayImpl {

    public static final String HOST_URL = "https://vision2.visionaitech.com.br";

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendMessage(String to, String message, String token, String instanceName) throws Exception {
        final RestTemplate restTemplate = new RestTemplate();

        String url = HOST_URL + "/message/sendText/" + instanceName;
        log.info("[ENVIANDO] Enviando mensagem: {}, para {} via {}", message, to, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        Map<String, String> payload = new HashMap<>();
        payload.put("number", to);
        payload.put("text", message);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<WhatsAppResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, WhatsAppResponse.class
        );

        log.info("[SUCESSO] Mensagem enviada para {} - Status: {}", to, response.getStatusCode());
        return response.getBody();
    }

    public void typingMessage(String number, Integer delay, String token, String instanceName) {
        final RestTemplate restTemplate = new RestTemplate();

        String url = HOST_URL + "/chat/sendPresence/" + instanceName;
        log.info("[TYPING] Enviando sinal de 'digitando' para {} via {}", number, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        Map<String, Object> payload = new HashMap<>();
        payload.put("number", number);
        payload.put("delay", delay);
        payload.put("presence", "composing");

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

        String url = HOST_URL + "/message/sendWhatsAppAudio/" + instanceName;
        log.info("[AUDIO] Enviando áudio para {} via {}", to, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        Map<String, Object> payload = new HashMap<>();
        payload.put("number", to);
        payload.put("audio", base64Audio);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("[AUDIO] Áudio enviado com sucesso para {} - Status: {}", to, response.getStatusCode());
    }

    @Retryable(
            value = {HttpServerErrorException.class, HttpClientErrorException.class, SocketTimeoutException.class},
            maxAttemptsExpression = "${whatsapp.retry.maxAttempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public WhatsAppResponse sendLocation(String to, String name, String address, double latitude, double longitude, String token, String instanceName) throws Exception {
        final RestTemplate restTemplate = new RestTemplate();

        String url = HOST_URL + "/message/sendLocation/" + instanceName;
        log.info("[ENVIANDO] Enviando localização: {}, {}, {} para {} via {}", name, address, latitude + "," + longitude, to, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        Map<String, Object> payload = new HashMap<>();
        payload.put("number", to);
        payload.put("name", name);
        payload.put("address", address);
        payload.put("latitude", latitude);
        payload.put("longitude", longitude);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<WhatsAppResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, WhatsAppResponse.class
        );

        log.info("[SUCESSO] Localização enviada para {} - Status: {}", to, response.getStatusCode());
        return response.getBody();
    }

    public List<WhatsAppGroupResponse> fetchAllGroups(String token, String instanceName, boolean getParticipants) throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        String url = HOST_URL + "/group/fetchAllGroups/" + instanceName + "?getParticipants=" + getParticipants;
        log.info("[FETCH GROUPS] Buscando grupos para {} via {}", instanceName, url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class
        );

        log.info("[FETCH GROUPS] Resposta recebida - Status: {}", response.getStatusCode());
        String responseBody = response.getBody();
        log.info("groups: {}", responseBody);
        ObjectMapper mapper = new ObjectMapper();
        var result = mapper.readValue(responseBody, new TypeReference<>() {
        });

        log.info("result: {}", result);

        return (List<WhatsAppGroupResponse>) result;
    }

    public void deleteInstance(String instanceName) {
        final RestTemplate restTemplate = new RestTemplate();
        String url = HOST_URL + "/instance/delete/" + instanceName;
        log.info("[DELETE INSTANCE] Deletando instância: {} via {}", instanceName, url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", "vision-api-key");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, request, String.class
            );
            log.info("[DELETE INSTANCE] Instância {} deletada com sucesso - Status: {}", instanceName, response.getStatusCode());
        } catch (Exception e) {
            log.error("[DELETE INSTANCE][ERRO] Falha ao deletar instância {}: {}", instanceName, e.getMessage(), e);
        }
    }

    public InstanceResponse createInstance(String token, String instanceName, String number){
        final RestTemplate restTemplate = new RestTemplate();
        String url = HOST_URL + "/instance/create";
        log.info("[CREATE INSTANCE] Criando instância: {} via {}", instanceName, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", "vision-api-key"); // se quiser, use `headers.set("apikey", token);`

        // Webhook config
        Webhook webhook = Webhook.builder()
                .url("https://vision2.visionaitech.com.br/api-message-processor/api/message/webhook/receive-message")
                .byEvents(false)
                .base64(false)
                .headers(Map.of("Content-Type", "application/json"))
                .events(List.of("MESSAGES_UPSERT"))
                .build();

        // Payload config
        CreateInstanceRequest payload = CreateInstanceRequest.builder()
                .instanceName(instanceName)
                .token(token)
                .number(number)
                .qrcode(true)
                .integration("WHATSAPP-BAILEYS")
                .rejectCall(true)
                .groupsIgnore(true)
                .alwaysOnline(true)
                .readMessages(true)
                .webhook(webhook)
                .build();

        HttpEntity<CreateInstanceRequest> request = new HttpEntity<>(payload, headers);

        ResponseEntity<InstanceResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, InstanceResponse.class
        );

        log.info("[CREATE INSTANCE] Instância {} criada com sucesso - Status: {}", instanceName, response.getStatusCode());
        return response.getBody();
    }

    public CheckInstanceResponse checkInstance(String instanceName) {
        final RestTemplate restTemplate = new RestTemplate();
        String url = HOST_URL + "/instance/connectionState/" + instanceName;
        log.info("[CHECK INSTANCE] Verificando estado da instância: {} via {}", instanceName, url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", "vision-api-key");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<CheckInstanceResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, CheckInstanceResponse.class
            );
            log.info("[CHECK INSTANCE] Estado da instância {}: {}", instanceName, response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("[CHECK INSTANCE][404] Instância não encontrada: {} - Detalhes: {}", instanceName, e.getResponseBodyAsString());
                // Retorna objeto com instanceName e state = "desconected"
                CheckInstanceResponse resp = new CheckInstanceResponse();
                CheckInstanceResponse.InstanceInfo info = new CheckInstanceResponse.InstanceInfo();
                info.setInstanceName(instanceName);
                info.setState("disconnected");
                resp.setInstance(info);
                return resp;
            }
            log.error("[CHECK INSTANCE][ERRO] Falha ao verificar estado da instância {}: {}", instanceName, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("[CHECK INSTANCE][ERRO] Falha ao verificar estado da instância {}: {}", instanceName, e.getMessage(), e);
            return null;
        }
    }

    @Recover
    public WhatsAppResponse recoverSendMessage(Exception e, String to, String message, String token, String instanceName) {
        log.error("[RECOVER] Tentativas esgotadas ao enviar mensagem para {}: {}", to, e.getMessage(), e);
        return null;
    }

    @Recover
    public void recoverSendAudio(Exception e, String to, String base64Audio, String token, String instanceName) {
        log.error("[RECOVER] Tentativas esgotadas ao enviar áudio para {}: {}", to, e.getMessage(), e);
    }
}
