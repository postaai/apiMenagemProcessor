package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import lombok.Data;

@Data
public class MessageKey {
    private String remoteJid;
    private boolean fromMe;
    private String id;
}