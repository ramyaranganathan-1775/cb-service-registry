package com.igot.service_locator.health.service;


import com.igot.service_locator.exceptions.CustomException;
import com.igot.service_locator.util.ApiResponse;
import com.igot.service_locator.util.Constants;
import com.igot.service_locator.util.ProjectUtil;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthServiceImpl implements HealthService {


    @Autowired
    EntityManager entityManager;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private Logger log = LoggerFactory.getLogger(getClass().getName());



    @Override
    public ApiResponse checkHealthStatus(String requestId) throws Exception {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_HEALTH_CHECK);
        Map<String, Object> responseObj = new HashMap<>();
        response.getParams().setMsgId(requestId);
        response.getParams().setResMsgId(requestId);
        try {

            List<Map<String, Object>> healthResults = new ArrayList<>();
            redisHealthStatus(healthResults);
            postgresHealthStatus(healthResults);

            responseObj.put(Constants.CHECKS, healthResults);
            responseObj.put(Constants.NAME,Constants.ALL_HEALTH_CHECK);
            responseObj.put(Constants.HEALTHY, Constants.TRUE);
            response.put(Constants.RESPONSE, responseObj);

        } catch (Exception e) {
            log.error("Failed to process health check. Exception: ", e);
            response.put(Constants.HEALTHY, false);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErr(e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


    private void redisHealthStatus(List<Map<String, Object>> response) {

        Map<String, Object> result = ProjectUtil.createDefaultMapResponse(Constants.REDIS_CACHE,null,null);
        boolean isHealthy = true;

        try{
            isHealthy = isRedisHealthy();

            if (!isHealthy) {
                setErrorDetails( result, new CustomException(Constants.REDIS_CACHE +" Down", "Redis is unhealthy",
                        HttpStatus.SERVICE_UNAVAILABLE));
            }
        }catch (Exception e) {
            setErrorDetails( result, new CustomException(Constants.REDIS_CACHE +" Down", e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR));
        }

        response.add(result);


    }

    @Transactional(readOnly = true)
    public void postgresHealthStatus(List<Map<String, Object>> response) {
        Map<String, Object> result = ProjectUtil.createDefaultMapResponse(Constants.POSTGRES_DB,null,null);
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
        } catch (Exception e) {
            setErrorDetails( result, new CustomException(Constants.POSTGRES_DB +" Down", e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR));
        }
        response.add(result);

    }


    private void setErrorDetails(Map<String, Object> response, CustomException e) {

        response.put(Constants.HEALTHY,Constants.FALSE);
        response.put(Constants.ERR, e.getHttpStatusCode().value());
        response.put(Constants.ERROR_MESSAGE, e.getMessage()!=null ? e.getMessage() : e.getHttpStatusCode().getReasonPhrase());
    }

    public boolean isRedisHealthy() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }



}

