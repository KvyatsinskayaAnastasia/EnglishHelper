package com.learn.english.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@Slf4j
public class OllamaConfiguration {

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi())
                .defaultOptions(
                        OllamaOptions
                                .builder()
                                .model("llama3.1:8b")
                                .build())
                .build();
    }

    @Bean
    public OllamaApi ollamaApi() {
        String baseUrl = "http://ollama:11434";

        ReactorClientHttpRequestFactory clientHttpRequestFactory = new ReactorClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(30));
        clientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(30));
        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .webClientBuilder(WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector()))
                .restClientBuilder(RestClient.builder()
                        .requestFactory(clientHttpRequestFactory))
                .build();
    }
}
