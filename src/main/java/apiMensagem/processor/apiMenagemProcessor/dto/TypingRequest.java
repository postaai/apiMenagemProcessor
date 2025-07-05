package apiMensagem.processor.apiMenagemProcessor.dto;

public record TypingRequest(
        String number,
        Integer delay,
        String orgId
){}
