package apiMensagem.processor.apiMenagemProcessor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Configuration
@Slf4j
public class RetryLoggingConfig {

    @Bean
    public RetryListener retryLogger() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                // chamada final
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                int attempt = context.getRetryCount();
                log.warn("[RETRY][{}] Tentativa falhou: {}", attempt, throwable.getMessage());
            }
        };
    }
}
