package apiMensagem.processor.apiMenagemProcessor.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateSummaryRequest(
        @NotBlank(message = "orgId é obrigatório")
        @JsonProperty("orgId")
        String orgId,
        
        @NotBlank(message = "iterationId é obrigatório")
        @JsonProperty("iterationId")
        String iterationId
) {
}

