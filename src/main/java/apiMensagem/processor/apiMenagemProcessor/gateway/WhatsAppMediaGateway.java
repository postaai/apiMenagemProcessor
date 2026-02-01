package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gateway responsável por recuperar mídias (áudio, imagem, vídeo, documento)
 * conforme a documentação oficial da Meta (WhatsApp Cloud API).
 *
 * Fluxo oficial:
 * 1) GET /vXX.X/{media-id}  -> retorna JSON com "url"
 * 2) GET {url}             -> retorna o binário (Authorization: Bearer TOKEN)
 *
 * Doc oficial:
 * https://developers.facebook.com/documentation/business-messaging/whatsapp/guides/media
 */
@Component
public class WhatsAppMediaGateway {

    private final WebClient webClient;

    public WhatsAppMediaGateway() {
        this.webClient = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();
    }

    /**
     * STEP 1
     * Busca os metadados da mídia (principalmente a URL temporária).
     */
    public MediaMeta getMediaMeta(OrganizationsEntity organization) {

        return webClient.get()
                .uri("/v21.0/{mediaId}", organization.idMeta())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + organization.tokenMeta())
                .retrieve()
                .bodyToMono(MediaMeta.class)
                .block();
    }

    /**
     * STEP 2
     * Baixa o binário da mídia usando a URL retornada pela Meta.
     */
    public byte[] downloadMediaBinary(OrganizationsEntity organization, String mediaUrl) {


        return WebClient.builder()
                .build()
                .get()
                .uri(mediaUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + organization.tokenMeta())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    /**
     * DTO retornado pela Meta no GET /{media-id}
     *
     * Exemplo:
     * {
     *   "url": "https://lookaside.fbsbx.com/whatsapp_business/attachments/...",
     *   "mime_type": "audio/ogg; codecs=opus",
     *   "sha256": "...",
     *   "file_size": 12345
     * }
     */
    public record MediaMeta(
            String url,
            String mime_type,
            String sha256,
            Long file_size
    ) {
        public String mimeType() {
            return mime_type;
        }

        public Long fileSize() {
            return file_size;
        }
    }

}
