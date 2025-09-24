package apiMensagem.processor.apiMenagemProcessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QrCodePayload {

    private String organizationId;
    private String pairingCode;
    private String code;
    private String qrImage;
}
