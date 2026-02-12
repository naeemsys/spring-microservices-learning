package com.example.auth_service.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final long accessTtl;
    private final long refreshTtl;
    private final String keyId;

    private final RSAKey jwk;  // For JWKS endpoint

    public JwtService(org.springframework.core.env.Environment env, ResourceLoader resourceLoader) throws Exception {

        this.keyId = env.getProperty("auth.jwt.key-id", "demo-key-1");
        this.accessTtl = Long.parseLong(env.getProperty("auth.jwt.access-token-ttl-sec", "300"));
        this.refreshTtl = Long.parseLong(env.getProperty("auth.jwt.refresh-token-ttl-sec", "1209600"));

        // --- Load PRIVATE KEY (PKCS8) ---
        String privatePemLocation = env.getProperty("auth.jwt.private-key-location", "classpath:private.pem");
        Resource privateResource = resourceLoader.getResource(privatePemLocation);
        String privatePem = new String(privateResource.getInputStream().readAllBytes());
        privatePem = privatePem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] privateBytes = Base64.getDecoder().decode(privatePem);
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = (RSAPrivateKey) kf.generatePrivate(privateSpec);

        // --- Load PUBLIC KEY (X509) ---
        String publicPemLocation = env.getProperty("auth.jwt.public-key-location", "classpath:jwk-public.pem");
        Resource publicResource = resourceLoader.getResource(publicPemLocation);
        String publicPem = new String(publicResource.getInputStream().readAllBytes());
        publicPem = publicPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] publicBytes = Base64.getDecoder().decode(publicPem);
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);
        this.publicKey = (RSAPublicKey) kf.generatePublic(publicSpec);

        // --- Build JWK for JWKS endpoint ---
        this.jwk = new RSAKey.Builder(this.publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(this.keyId)
                .build();
    }

    public String createAccessToken(String username, List<String> roles, String clientId) throws JOSEException {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("http://localhost:9000")
                .audience(clientId == null ? "api" : clientId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTtl)))
                .claim("roles", roles)
                .claim("scope", "openid")
                .build();

        SignedJWT signedJWT =
                new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(this.keyId).build(), claims);

        signedJWT.sign(new RSASSASigner(privateKey));

        return signedJWT.serialize();
    }

    public String createRefreshToken(String username, String clientId) throws JOSEException {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("http://localhost:9000")
                .audience(clientId == null ? "api" : clientId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(refreshTtl)))
                .claim("scope", "offline_access")
                .build();

        SignedJWT signedJWT =
                new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(this.keyId).build(), claims);

        signedJWT.sign(new RSASSASigner(privateKey));

        return signedJWT.serialize();
    }

    public Map<String, Object> getJwks() {
        return new JWKSet(this.jwk).toJSONObject();
    }
}
