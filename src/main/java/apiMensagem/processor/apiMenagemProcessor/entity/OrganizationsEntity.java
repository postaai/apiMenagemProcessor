package apiMensagem.processor.apiMenagemProcessor.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.List;

@Document("organizations")
public record OrganizationsEntity(
        @Id
        String id,
        String orgId,
        String idMeta,
        String numberIdMeta,
        String tokenAgenda,
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
        // ... added scheduling fields ...
        List<BusinessHour> businessHours,
        String timezone,
        Integer slotDurationMinutes,
        Integer bufferBetweenMeetingsMinutes,
        Integer minAdvanceBookingHours,
        Integer maxAdvanceBookingDays,
        Boolean enableGoogleMeet,
        // ...existing code...
        Map<String, SummaryField> sumarySchema,
        String sumaryWebHookUrl
) {
    public record SummaryField(
            String type,
            String label
    ) {
    }

    public record BusinessHour(
            Integer dayOfWeek,
            Boolean isWorkDay,
            Integer startHour,
            Integer startMinute,
            Integer endHour,
            Integer endMinute
    ) {
    }
}