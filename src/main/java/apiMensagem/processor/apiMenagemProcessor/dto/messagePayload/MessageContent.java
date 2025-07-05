package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageContent {

    private String conversation;

    @JsonProperty("imageMessage")
    private MediaMessage imageMessage;

    @JsonProperty("audioMessage")
    private MediaMessage audioMessage;

    private MessageContextInfo messageContextInfo;
}