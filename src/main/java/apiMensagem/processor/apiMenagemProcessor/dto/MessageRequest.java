package apiMensagem.processor.apiMenagemProcessor.dto;

public record MessageRequest(
        String orgId,
        String userId,
        String text
) {
}
