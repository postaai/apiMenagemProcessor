package apiMensagem.processor.apiMenagemProcessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Webhook {
    private String url;
    private boolean byEvents;
    private boolean base64;
    private Map<String, String> headers;
    private List<String> events;
}
