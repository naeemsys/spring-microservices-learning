package com.example.order_service.client;


import com.example.order_service.config.FeignSecurityConfig;
import com.example.order_service.dto.UserRequest;
import com.example.order_service.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", configuration = FeignSecurityConfig.class)
public interface UserClient {

//    @CircuitBreaker(name = "userService", fallbackMethod = "fallback")
    @GetMapping("/api/users/{id}")
    UserResponse getUser(@PathVariable("id") Long id);

//    default UserResponse fallback(Long id, Throwable ex) {
//        // In this we can throw exception or other logic depends on the use cases
//        return new UserResponse(id, "GUEST", "guest@mail.com");
//    }

    @PostMapping("/api/users")
    UserResponse createUser(@RequestBody UserRequest request);

}
