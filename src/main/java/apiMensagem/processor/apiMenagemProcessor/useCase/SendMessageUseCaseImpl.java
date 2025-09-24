package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import apiMensagem.processor.apiMenagemProcessor.entity.PlatformEnum;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayImpl;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGatewayMetaImpl;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystemNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendMessageUseCaseImpl implements SendMessageUseCase {

    private final OrganizationRepository repository;
    private final WhatsAppGatewayImpl whatsAppGatewayEvolution;
    private final WhatsAppGatewayMetaImpl whatsAppGatewayMeta;

    @Override
    public void sendMessageWhatsApp(MessageRequest request) {
        try {

            var organization = repository.findByorgId(request.orgId())
                    .orElseThrow(FileSystemNotFoundException::new);

            switch (organization.platform()) {
                case EVOLUTION ->
                        whatsAppGatewayEvolution.sendMessage(request.userId(), request.text(), organization.token(), organization.instanceName());
                case META ->
                        whatsAppGatewayMeta.sendMessage(request.userId(), request.text(), organization.token(), organization.instanceName(), organization.numberIdMeta());
                default -> throw new IllegalArgumentException("Plataforma não encontrada " + organization.platform());
            }
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }

    @Override
    public void typingMessage(TypingRequest request) {
        try {

            var organization = repository.findByorgId(request.orgId())
                    .orElseThrow(FileSystemNotFoundException::new);

            switch (organization.platform()) {
                case EVOLUTION ->
                        whatsAppGatewayEvolution.typingMessage(request.number(), request.delay(), organization.token(), organization.instanceName());
                case META ->
                        whatsAppGatewayMeta.typingMessage(request.number(), request.delay(), organization.token(), organization.instanceName());
                default -> throw new IllegalArgumentException("Plataforma não encontrada " + organization.platform());
            }
        } catch (Exception e) {
            throw new FileSystemNotFoundException();

        }
    }

    @Override
    public void sendAudio(AudioRequest request) {
        try {
            var organization = repository.findByorgId(request.orgId())
                    .orElseThrow(FileSystemNotFoundException::new);

            switch (organization.platform()) {
                case EVOLUTION ->
                        whatsAppGatewayEvolution.sendAudio(request.number(), request.audio(), organization.token(), organization.instanceName());
                case META ->
                        whatsAppGatewayMeta.sendAudio(request.number(), request.audio(), organization.token(), organization.instanceName(), organization.numberIdMeta());
                default -> throw new IllegalArgumentException("Plataforma não encontrada " + organization.platform());
            }
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }

    }

    @Override
    public void sendLocation(LocationRequest request) {
        try {

            var organization = repository.findByorgId(request.orgId())
                    .orElseThrow(FileSystemNotFoundException::new);

            switch (organization.platform()) {
                case EVOLUTION ->
                        whatsAppGatewayEvolution.sendLocation(request.userId(), request.name(), request.address(), request.latitude(), request.longitude(), organization.token(), organization.instanceName());
                case META ->
                        whatsAppGatewayMeta.sendLocation(request.userId(), request.name(), request.address(), request.latitude(), request.longitude(), organization.token(), organization.numberIdMeta());
                default -> throw new IllegalArgumentException("Plataforma não encontrada " + organization.platform());
            }
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }

    @Override
    public List<WhatsAppGroupResponse> getWhatsAppGroups(String orgId, boolean participants) {
        try {
            var organization = repository.findByorgId(orgId)
                    .orElseThrow(FileSystemNotFoundException::new);

            if (PlatformEnum.EVOLUTION.equals(organization.platform())) {
                return whatsAppGatewayEvolution.fetchAllGroups(organization.token(), organization.instanceName(), participants);
            }

            throw new IllegalArgumentException("Meta não possui grupos na Cloud API");

        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }

    @Override
    public QrCodePayload generateQRCode(String orgId) {
        try {
            var organization = repository.findByorgId(orgId)
                    .orElseThrow(FileSystemNotFoundException::new);

            if (PlatformEnum.EVOLUTION.equals(organization.platform())) {
                 whatsAppGatewayEvolution.deleteInstance(organization.instanceName());
                Thread.sleep(1000);
                 var qrCodeResponse = whatsAppGatewayEvolution.createInstance(organization.token(), organization.instanceName(), organization.numberId());
                 return QrCodePayload.builder()
                         .organizationId(organization.orgId())
                         .pairingCode(qrCodeResponse.getQrcode().getPairingCode())
                         .code(qrCodeResponse.getQrcode().getCode())
                         .qrImage(qrCodeResponse.getQrcode().getBase64())
                         .build();
            }

            throw new IllegalArgumentException("Meta não possui QR Code na Cloud API");

        } catch (Exception e) {
            log.info("Erro ao gerar QR Code: {}", e.getMessage());
            throw new FileSystemNotFoundException();
        }
    }
}