package com.igot.service_locator.health.service;

import com.igot.service_locator.util.ApiResponse;
import com.igot.service_locator.util.Constants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {


    // Dependencies
    @Mock
    private RedisTemplate<String, Object> redisTemplateMock;
    @Mock
    private RedisConnectionFactory connectionFactoryMock;
    @Mock
    private RedisConnection redisConnectionMock;

    @Mock EntityManager entityManager;
    @Mock Query query;
    @InjectMocks
    HealthServiceImpl healthService;
    @Mock
    ApiResponse response;
    private final String REQUEST_ID = "test-request--123";


    // 🔹 Common mocks
    void mockAllHealthy() throws Exception {

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);

    }


    // ✅ SUCCESS CASE
    @Test
    void testHealthCheckSuccess() throws Exception {

        mockAllHealthy();
        response = healthService.checkHealthStatus(REQUEST_ID);
        assertNotNull(response);
        Map<String, Object> result =
                (Map<String, Object>) response.get(Constants.RESPONSE);
        assertNotNull(response);
        assertEquals(Constants.ALL_HEALTH_CHECK, result.get(Constants.NAME));
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.get(Constants.CHECKS);
        assertEquals(2, checks.size());
    }

    // ❌ FAILURE CASE (Redis down)
    @Test
    void testRedisFailure() throws Exception {

        mockAllHealthy();
        when(redisTemplateMock.getConnectionFactory()).thenReturn(connectionFactoryMock);
        when(connectionFactoryMock.getConnection()).thenReturn(redisConnectionMock);
        when(redisConnectionMock.ping()).thenReturn("FAIL");
       // when(healthService.isRedisHealthy()).thenReturn(false);

        response = healthService.checkHealthStatus(REQUEST_ID);

        assertFalse(Boolean.TRUE.equals(response.get(Constants.HEALTHY)));
    }

    // ❌ FAILURE CASE (Postgres down)
    @Test
    void testPostgresFailure() throws Exception {

        mockAllHealthy();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("DB error"));

        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        assertFalse(Boolean.TRUE.equals(response.get(Constants.HEALTHY)));
    }

    @Test
    void testExceptionHandling() throws Exception {

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("DB failure"));

        ApiResponse response = healthService.checkHealthStatus("req-ex");

        assertNotNull(response);

        // ✅ overall unhealthy
        assertFalse(Boolean.TRUE.equals(response.get(Constants.HEALTHY)));

        // ✅ exception handled
        assertNotEquals(Constants.FAILED, response.getParams().getStatus());

    }



}