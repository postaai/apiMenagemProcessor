package apiMensagem.processor.apiMenagemProcessor.useCase;

import apiMensagem.processor.apiMenagemProcessor.repository.OrganizationRepository;
import apiMensagem.processor.apiMenagemProcessor.dto.MessageRequest;
import apiMensagem.processor.apiMenagemProcessor.gateway.WhatsAppGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystemNotFoundException;

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
}
