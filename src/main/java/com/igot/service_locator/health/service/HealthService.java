package com.igot.service_locator.health.service
;

import com.igot.service_locator.util.ApiResponse;


public interface HealthService {

    ApiResponse checkHealthStatus(String requestId) throws Exception;

}
