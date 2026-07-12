package com.learn.english.configuration;

import com.learn.english.user.handler.UserStatusHandler;
import com.learn.english.model.UserStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class UserStateConfiguration {
    @Bean
    public Map<UserStatus, UserStatusHandler> userStatusHandlers(List<UserStatusHandler> userStatusHandlers) {
        return userStatusHandlers.stream().collect(Collectors.toMap(UserStatusHandler::getUserStatus, Function.identity()));
    }
}
