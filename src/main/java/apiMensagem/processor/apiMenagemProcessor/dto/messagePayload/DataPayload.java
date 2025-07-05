package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataPayload {

    private MessageKey key;
    private String pushName;
    private String status;
    private MessageContent message;

    private String messageType;
    private Long messageTimestamp;
    private String instanceId;
    private String source;
    private Object contextInfo; // pode ser refinado se necessário

}