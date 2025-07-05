package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import lombok.Data;

@Data
public class MessageContextInfo {
    private DeviceListMetadata deviceListMetadata;
    private Integer deviceListMetadataVersion;
    private String messageSecret;
}