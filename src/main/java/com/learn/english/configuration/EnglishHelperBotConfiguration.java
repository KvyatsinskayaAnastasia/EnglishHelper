package com.learn.english.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.english.configuration.properties.BotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Random;

@Configuration
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class EnglishHelperBotConfiguration {

    private final BotProperties botProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean TelegramClient telegramClient() {
        return new OkHttpTelegramClient(botProperties.getToken());
    }

    @Bean
    Random random() {
        return new Random();
    }
}
