package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.dto.*;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGateway;
import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystemNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SendMessageUseCaseImpl implements SendMessageUseCase {

    private final OrganizationRepository repository;
    private final WhatsAppGateway whatsAppGateway;

    @Override
    public void sendMessageWhatsApp(MessageRequest request) {

        var organization = repository.findByorgId(request.orgId())
                .orElseThrow(FileSystemNotFoundException::new);

        try {
            whatsAppGateway.sendMessage(request.userId(), request.text(), organization.token(), organization.instanceName());
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }

    @Override
    public void typingMessage(TypingRequest request) {

        var organization = repository.findByorgId(request.orgId())
                .orElseThrow(FileSystemNotFoundException::new);

        try {
            whatsAppGateway.typingMessage(request.number(), request.delay(), organization.token(), organization.instanceName());
        } catch (Exception e) {
            throw new FileSystemNotFoundException();

        }
    }

    @Override
    public void sendAudio(AudioRequest request) {

        var organization = repository.findByorgId(request.orgId())
                .orElseThrow(FileSystemNotFoundException::new);

        try {
            whatsAppGateway.sendAudio(request.number(), request.audio(), organization.token(), organization.instanceName());
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }

    }

    @Override
    public void sendLocation(LocationRequest request) {

        var organization = repository.findByorgId(request.orgId())
                .orElseThrow(FileSystemNotFoundException::new);

        try {
            whatsAppGateway.sendLocation(request.userId(), request.name(), request.address(), request.latitude(), request.longitude(), organization.token(), organization.instanceName());
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }

    @Override
    public List<WhatsAppGroupResponse> getWhatsAppGroups(String orgId, boolean participants) {

        var organization = repository.findByorgId(orgId)
                .orElseThrow(FileSystemNotFoundException::new);

        try {
            return whatsAppGateway.fetchAllGroups(organization.token(), organization.instanceName(), participants);
        } catch (Exception e) {
            throw new FileSystemNotFoundException();
        }
    }
}
