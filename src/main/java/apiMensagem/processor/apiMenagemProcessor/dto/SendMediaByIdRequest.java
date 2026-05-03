package apiMensagem.processor.apiMenagemProcessor.dto;

public record SendMediaByIdRequest(
        String orgId,
        String number,
        String mediaId,
        String type,
        String caption
) {
}