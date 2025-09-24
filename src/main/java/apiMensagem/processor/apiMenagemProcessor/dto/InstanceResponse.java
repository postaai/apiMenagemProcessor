package apiMensagem.processor.apiMenagemProcessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstanceResponse {
    @JsonProperty("instance")
    private Instance instance;
    @JsonProperty("hash")
    private String hash;
    @JsonProperty("webhook")
    private Webhook webhook;
    @JsonProperty("websocket")
    private Websocket websocket;
    @JsonProperty("rabbitmq")
    private Rabbitmq rabbitmq;
    @JsonProperty("nats")
    private Nats nats;
    @JsonProperty("sqs")
    private Sqs sqs;
    @JsonProperty("settings")
    private Settings settings;
    @JsonProperty("qrcode")
    private Qrcode qrcode;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Instance {
        @JsonProperty("instanceName")
        private String instanceName;
        @JsonProperty("instanceId")
        private String instanceId;
        @JsonProperty("integration")
        private String integration;
        @JsonProperty("webhookWaBusiness")
        private String webhookWaBusiness;
        @JsonProperty("accessTokenWaBusiness")
        private String accessTokenWaBusiness;
        @JsonProperty("status")
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Webhook {
        @JsonProperty("webhookUrl")
        private String webhookUrl;
        @JsonProperty("webhookHeaders")
        private WebhookHeaders webhookHeaders;
        @JsonProperty("webhookByEvents")
        private boolean webhookByEvents;
        @JsonProperty("webhookBase64")
        private boolean webhookBase64;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookHeaders {
        @JsonProperty("Content-Type")
        private String contentType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Websocket {
        // Vazio conforme JSON
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rabbitmq {
        // Vazio conforme JSON
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nats {
        // Vazio conforme JSON
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sqs {
        // Vazio conforme JSON
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Settings {
        @JsonProperty("rejectCall")
        private boolean rejectCall;
        @JsonProperty("msgCall")
        private String msgCall;
        @JsonProperty("groupsIgnore")
        private boolean groupsIgnore;
        @JsonProperty("alwaysOnline")
        private boolean alwaysOnline;
        @JsonProperty("readMessages")
        private boolean readMessages;
        @JsonProperty("readStatus")
        private boolean readStatus;
        @JsonProperty("syncFullHistory")
        private boolean syncFullHistory;
        @JsonProperty("wavoipToken")
        private String wavoipToken;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Qrcode {
        @JsonProperty("pairingCode")
        private String pairingCode;
        @JsonProperty("code")
        private String code;
        @JsonProperty("base64")
        private String base64;
        @JsonProperty("count")
        private int count;
    }
}
