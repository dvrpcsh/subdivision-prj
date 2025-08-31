package com.subdivision.subdivision_prj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Redis 데이터베이스 연결 및 사용을 위한 Spring 설정 클래스입니다.
 * 이 클래스는 Spring Boot의 기본 Redis 설정을 재정의하여,
 * 더 안정적이고 성능이 뛰어난 'Jedis Connection Pool' 방식을 사용하도록 구성합니다.
 * @author subdivision
 */
@Configuration
public class RedisConfig {

    // application.properties에서 Redis 서버의 호스트 주소를 주입받습니다.
    @Value("${spring.redis.host:redis}")
    private String redisHost;

    // application.properties에서 Redis 서버의 포트 번호를 주입받습니다.
    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Jedis 라이브러리를 사용하여 Redis에 연결하기 위한 Connection Factory를 Bean으로 등록합니다.
     * @return JedisConnectionFactory 인스턴스
     */
    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        // 1. Redis 서버의 기본 연결 정보(호스트, 포트)를 설정합니다.
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(0); // 사용할 데이터베이스 인덱스 (기본값: 0)

        // 2. [핵심] Jedis Connection Pool의 상세 설정을 구성합니다.
        // Connection Pool은 미리 여러 개의 연결을 만들어두고 재사용하는 기술로,
        // 매번 연결을 새로 맺는 비용을 줄여 애플리케이션 성능을 크게 향상시킵니다.
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);      // 풀이 유지할 수 있는 최대 커넥션 수
        poolConfig.setMaxIdle(8);       // 풀에서 유지할 수 있는 최대 유휴(idle) 커넥션 수
        poolConfig.setMinIdle(2);       // 풀에서 유지해야 하는 최소 유휴 커넥션 수
        poolConfig.setMaxWait(Duration.ofSeconds(10)); // 풀이 가득 찼을 때, 커넥션을 얻기 위해 대기하는 최대 시간
        poolConfig.setTestOnBorrow(true); // 풀에서 커넥션을 가져올 때마다 연결이 유효한지 테스트합니다. (안정성 향상)
        poolConfig.setTestOnReturn(true); // 풀에 커넥션을 반납할 때마다 연결이 유효한지 테스트합니다.

        // 3. 위에서 만든 기본 설정과 풀 설정을 사용하여 JedisConnectionFactory를 생성합니다.
        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        factory.setPoolConfig(poolConfig); // 풀 설정 적용
        factory.afterPropertiesSet(); // 설정 값 검증 및 초기화
        return factory;
    }

    /**
     * Spring에서 Redis 데이터에 쉽게 접근하고 조작할 수 있도록 도와주는 RedisTemplate을 Bean으로 등록합니다.
     * @return RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory()); // 위에서 생성한 Connection Factory를 사용하도록 설정

        // [중요] Redis에 데이터를 저장할 때, Key와 Value를 어떤 형식으로 변환(직렬화)할지 설정합니다.
        // 이 설정을 통해, Java 객체를 사람이 읽을 수 있는 형태로 Redis에 저장하고, 다시 Java 객체로 쉽게 변환할 수 있습니다.

        // Key는 일반적으로 문자열이므로 String으로 직렬화합니다.
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Value는 다양한 타입의 Java 객체가 저장될 수 있으므로, JSON 형식으로 직렬화합니다.
        // GenericJackson2JsonRedisSerializer는 객체를 JSON 문자열로, JSON 문자열을 다시 원래 객체로 변환해줍니다.
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 기본 직렬화 방식을 String으로 설정합니다.
        redisTemplate.setDefaultSerializer(stringSerializer);

        redisTemplate.afterPropertiesSet(); // 설정 값 검증 및 초기화
        return redisTemplate;
    }
}
