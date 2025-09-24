package apiMensagem.processor.apiMenagemProcessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateInstanceRequest {
    private String instanceName;
    private String token;
    private String number;
    private boolean qrcode;
    private String integration;

    // settings
    private boolean rejectCall;
    private boolean groupsIgnore;
    private boolean alwaysOnline;
    private boolean readMessages;

    private Webhook webhook;
}