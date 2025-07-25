package com.bootcamptoprod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@SpringBootApplication
public class SpringAIAudioAnalysisCookbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAIAudioAnalysisCookbookApplication.class, args);
    }

    /**
     * Configures a RestClientCustomizer bean to integrate Logbook for HTTP logging.
     * This bean adds an interceptor to all outgoing REST client calls made by Spring,
     * allowing us to log the requests sent to the AI model and the responses received.
     */
    @Bean
    public RestClientCustomizer restClientCustomizer(Logbook logbook) {
        return restClientBuilder -> restClientBuilder.requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook));
    }
}
