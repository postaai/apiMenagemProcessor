package apiMensagem.processor.apiMenagemProcessor.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("organizations")
public record OrganizationsEntity(
        @Id
        String id,
        String orgId,
        String idMeta,
        String numberIdMeta,
        String numberId,
        String name,
        String apiKey,
        String instanceName,
        String tokenMeta,
        String token,
        Instant createdAt,
        Instant updatedAt,
        Integer __v,
        String socketId,
        String prompt,
        String modelName,
        String mcpServerPath,
        PlatformEnum platform,
        Map<String, SummaryField> sumarySchema,
        String sumaryWebHookUrl
) {
    public record SummaryField(
            String type,
            String label
    ) {
    }
}