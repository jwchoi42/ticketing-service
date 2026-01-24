package dev.ticketing.common.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheMananger(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultRedisCacheConfiguration())
                .build();
    }

    private RedisCacheConfiguration defaultRedisCacheConfiguration() {
        // 커스텀 ObjectMapper 생성 (JavaTimeModule 등록)
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // 커스텀 ObjectMapper를 사용하는 Serializer 생성
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(fromSerializer(keySerializer))
                .serializeValuesWith(fromSerializer(valueSerializer));
    }

    private <T> RedisSerializationContext.SerializationPair<T> fromSerializer(RedisSerializer<T> serializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer(serializer);
    }
}
