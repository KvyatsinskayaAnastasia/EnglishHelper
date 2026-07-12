package com.learn.english.configuration;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class BotActionConfiguration {
    @Bean
    public Map<BotAction, BotActionHandler> botActionHandlers(List<BotActionHandler> botActionHandlers) {
        return botActionHandlers.stream().collect(Collectors.toMap(BotActionHandler::getBotAction, Function.identity()));
    }
}
