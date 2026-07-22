package apiMensagem.processor.apiMenagemProcessor.dto;

public record MediaLinkRequest(
        String orgId,
        String number,
        String type,
        String link,
        String caption,
        String filename,
        Boolean voice
) {
}