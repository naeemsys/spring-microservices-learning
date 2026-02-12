package com.example.user_service.controller;

import com.example.user_service.dto.UserRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.entity.User;
import com.example.user_service.exception.NotFoundException;
import com.example.user_service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService svc;

  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  public UserController(UserService svc){ this.svc = svc; }

  @PostMapping
  public ResponseEntity<UserResponse> create(@RequestBody UserRequest request){
    User u = new User();
    u.setEmail(request.email());
    u.setUsername(request.username());
    User saved = svc.create(u);
    return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(UserResponse.fromEntity(saved));
  }

  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> get(@PathVariable Long id){
      log.debug("Fetching User By Id: getUser({})", id);
      return svc.findById(id)
      .map(user -> ResponseEntity.ok(UserResponse.fromEntity(user)))
      .orElseThrow(() -> new NotFoundException("User not found: " + id));
  }

  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
      return ResponseEntity.ok(Map.of("sub", jwt.getSubject(), "roles", jwt.getClaimAsStringList("roles")));
  }

}
