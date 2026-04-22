package apiMensagem.processor.apiMenagemProcessor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenerateSummaryResponse {
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("data")
    private SummaryData data;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SummaryData {
        @JsonProperty("summaryId")
        private String summaryId;
        
        @JsonProperty("iterationId")
        private String iterationId;
        
        @JsonProperty("orgId")
        private String orgId;
        
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("sumaryText")
        private String sumaryText;
        
        @JsonProperty("leadScore")
        private Integer leadScore;
        
        @JsonProperty("data")
        private Map<String, Object> data;
        
        @JsonProperty("createdAt")
        private String createdAt;
    }
}

