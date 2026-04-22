package apiMensagem.processor.apiMenagemProcessor.exception;

public class OrganizacaoInativaException extends RuntimeException {
    public OrganizacaoInativaException() {
        super("Organização inativa");
    }
}