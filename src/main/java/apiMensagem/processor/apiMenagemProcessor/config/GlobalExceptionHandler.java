package apiMensagem.processor.apiMenagemProcessor.config;

import apiMensagem.processor.apiMenagemProcessor.exception.OrganizacaoInativaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrganizacaoInativaException.class)
    public ResponseEntity<String> handleOrganizacaoInativa(OrganizacaoInativaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}