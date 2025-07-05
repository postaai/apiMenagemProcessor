package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import lombok.Data;

@Data
public class DeviceListMetadata {
    private String senderKeyHash;
    private String senderTimestamp;
    private String recipientKeyHash;
    private String recipientTimestamp;
}