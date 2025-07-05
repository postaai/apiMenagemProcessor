package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookMessagePayload {

    private String event;
    private String instance;
    private DataPayload data;
    private String destination;

    @JsonProperty("date_time")
    private String dateTime;

    private String sender;

    @JsonProperty("server_url")
    private String serverUrl;

    private String apikey;

}