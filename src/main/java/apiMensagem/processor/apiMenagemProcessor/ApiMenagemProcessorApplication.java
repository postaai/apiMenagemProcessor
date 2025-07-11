package apiMensagem.processor.apiMenagemProcessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class ApiMenagemProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMenagemProcessorApplication.class, args);
	}

}
