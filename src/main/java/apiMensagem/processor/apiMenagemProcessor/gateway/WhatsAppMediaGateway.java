package apiMensagem.processor.apiMenagemProcessor.gateway;

import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMediaGateway.class);

    private final WebClient graphClient;
    private final WebClient downloadClient;

    public WhatsAppMediaGateway() {
        this.graphClient = WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();

        this.downloadClient = WebClient.builder().build(); // URL vem completa no download
    }

    /**
     * STEP 1
     * Busca os metadados da mídia (principalmente a URL temporária).
     *
     * IMPORTANTE: o parâmetro aqui precisa ser o MEDIA_ID (ex.: 1433154418447052),
     * e não o idMeta da organização (WABA/phoneNumberId).
     */
    public MediaMeta getMediaMeta(OrganizationsEntity organization, String mediaId) {

        if (mediaId == null || mediaId.isBlank()) {
            log.error("[META][MEDIA][META][ERRO] mediaId vazio/nulo");
            return null;
        }

        // Ajuste a versão se você quiser padronizar em outro lugar
        String version = "v22.0";

        log.info("[META][MEDIA][META][IN] orgId={} mediaId={} version={}",
                organization != null ? organization.orgId() : null,
                mediaId,
                version);

        return graphClient.get()
                .uri("/{version}/{mediaId}", version, mediaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + organization.tokenMeta())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][MEDIA][META][ERRO] mediaId={} status={} body={}",
                                    mediaId, resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta getMediaMeta erro: " + body));
                        })
                )
                .bodyToMono(MediaMeta.class)
                .doOnNext(meta -> log.info("[META][MEDIA][META][OK] mediaId={} urlPresent={} mimeType={}",
                        mediaId,
                        meta != null && meta.url != null && !meta.url.isBlank(),
                        meta != null ? meta.mime_type : null
                ))
                .block();
    }

    /**
     * STEP 2
     * Baixa o binário da mídia usando a URL retornada pela Meta.
     */
    public byte[] downloadMediaBinary(OrganizationsEntity organization, String mediaUrl) {

        if (mediaUrl == null || mediaUrl.isBlank()) {
            log.error("[META][MEDIA][DOWNLOAD][ERRO] mediaUrl vazio/nulo");
            return null;
        }

        log.info("[META][MEDIA][DOWNLOAD][IN] orgId={} url={}",
                organization != null ? organization.orgId() : null,
                mediaUrl);

        return downloadClient.get()
                .uri(mediaUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + organization.tokenMeta())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.error("[META][MEDIA][DOWNLOAD][ERRO] status={} body={}",
                                    resp.statusCode().value(), body);
                            return Mono.error(new RuntimeException("Meta downloadMediaBinary erro: " + body));
                        })
                )
                .bodyToMono(byte[].class)
                .doOnNext(bytes -> log.info("[META][MEDIA][DOWNLOAD][OK] bytes={}",
                        bytes != null ? bytes.length : 0
                ))
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
        public String mimeType() { return mime_type; }
        public Long fileSize() { return file_size; }
    }
}
