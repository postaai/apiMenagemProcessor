package apiMensagem.processor.apiMenagemProcessor.dto;

public record LocationRequest(
        String orgId,
        String userId,
        Double latitude,
        Double longitude,
        String name,
        String address
) {
}
