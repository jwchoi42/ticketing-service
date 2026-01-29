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
    @org.springframework.context.annotation.Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultRedisCacheConfiguration())
                .build();
    }

    private RedisCacheConfiguration defaultRedisCacheConfiguration() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        // LocalDateTime 직렬화를 위한 JavaTimeModule 등록
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // GenericJackson2JsonRedisSerializer 기본 생성자와 동일한 타입 설정
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
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
