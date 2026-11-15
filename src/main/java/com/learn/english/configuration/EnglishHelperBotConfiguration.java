package com.learn.english.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.english.configuration.properties.BotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;

@Configuration
@EnableScheduling
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

    @Bean
    Clock clock(@Value("${bot.schedule.zone:Europe/Moscow}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
