package apiMensagem.processor.apiMenagemProcessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WhatsAppGroupResponse {
    private String id;
    private String subject;
    private String subjectOwner;
    private long subjectTime;
    private String pictureUrl;
    private int size;
    private long creation;
    private String owner;
    private boolean restrict;
    private boolean announce;
    private boolean isCommunity;
    private boolean isCommunityAnnounce;
    private List<Participant> participants;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Participant {
        private String id;
        private String admin;
    }
}
