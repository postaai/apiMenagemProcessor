package apiMensagem.processor.apiMenagemProcessor.dto.messagePayload;

import lombok.Data;

@Data
public class MediaMessage {
    private String url;
    private String mimetype;
    private String fileSha256;
    private String fileLength;
    private String mediaKey;
    private String fileEncSha256;
    private String directPath;
    private String mediaKeyTimestamp;

    // Extras específicos
    private Integer height;
    private Integer width;
    private Integer seconds;
    private Boolean ptt;
    private String waveform;
    private String jpegThumbnail;
}