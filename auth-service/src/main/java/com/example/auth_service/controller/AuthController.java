package com.example.auth_service.controller;

import com.example.auth_service.service.JwtService;
import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("")
public class AuthController {

  private final JwtService jwtService;

  // simple in-memory users (username -> password + roles) for demo
  private final Map<String, String> userStore = Map.of("alice", "password", "bob", "password");
  private final Map<String, List<String>> roles = Map.of("alice", List.of("ROLE_USER"), "bob", List.of("ROLE_USER","ROLE_ADMIN"));

  @Autowired
  public AuthController(JwtService jwtService){ this.jwtService = jwtService; }

  @PostMapping("/oauth/token")
  public ResponseEntity<?> token(@RequestParam String grant_type,
                                 @RequestParam(required = false) String username,
                                 @RequestParam(required = false) String password,
                                 @RequestParam(required = false) String refresh_token,
                                 @RequestParam(required = false) String client_id) throws JOSEException {
    if ("password".equals(grant_type)) {
      if (username == null || password == null) return ResponseEntity.badRequest().body("missing credentials");
      String pwd = userStore.get(username);
      if (pwd == null || !pwd.equals(password)) return ResponseEntity.status(401).body("invalid credentials");
      String access = jwtService.createAccessToken(username, roles.getOrDefault(username, List.of()), client_id);
      String refresh = jwtService.createRefreshToken(username, client_id);
      Map<String,Object> resp = new HashMap<>();
      resp.put("access_token", access);
      resp.put("token_type", "Bearer");
      resp.put("expires_in", 300);
      resp.put("refresh_token", refresh);
      return ResponseEntity.ok(resp);
    } else if ("refresh_token".equals(grant_type)) {
      // for demo: accept refresh_token (no revocation), in real world validate signature + expiry + revocation list
      // parse refresh token to get subject:
      // ... simplified:
      if (refresh_token == null) return ResponseEntity.badRequest().body("missing refresh_token");
      // skipping validation here — in production validate signed token
      // naive: simply issue new access token for subject "alice" (demo)
      // For demo assume refresh corresponds to alice
      String access = jwtService.createAccessToken("alice", roles.getOrDefault("alice", List.of()), client_id);
      Map<String,Object> resp = new HashMap<>();
      resp.put("access_token", access);
      resp.put("token_type", "Bearer");
      resp.put("expires_in", 300);
      return ResponseEntity.ok(resp);
    }
    return ResponseEntity.badRequest().body("unsupported grant_type");
  }

  @GetMapping("/.well-known/jwks.json")
  public ResponseEntity<Map<String,Object>> jwks(){
    return ResponseEntity.ok(jwtService.getJwks());
  }
}
