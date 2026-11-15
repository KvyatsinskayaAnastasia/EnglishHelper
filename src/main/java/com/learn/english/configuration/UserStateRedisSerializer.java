package com.learn.english.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learn.english.model.UserState;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

public final class UserStateRedisSerializer {
    private UserStateRedisSerializer() {
    }

    public static Jackson2JsonRedisSerializer<UserState> create() {
        return new Jackson2JsonRedisSerializer<>(objectMapper(), UserState.class);
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
