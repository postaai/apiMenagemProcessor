package apiMensagem.processor.apiMenagemProcessor.dto;

public record ImageLinkRequest(
        String orgId,
        String number,
        String link,
        String caption
) {
}