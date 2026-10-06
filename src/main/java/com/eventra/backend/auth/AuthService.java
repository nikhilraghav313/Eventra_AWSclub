package com.eventra.backend.auth;

import com.eventra.backend.user.LoginRequest;
import com.eventra.backend.user.User;
import com.eventra.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long accessTokenMinutes;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(accessTokenMinutes));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventra")
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .claim("role", user.getRole().name())
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String refreshTokenValue = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(refreshTokenValue));
        refreshToken.setExpiresAt(now.plus(Duration.ofDays(7)));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                Duration.ofMinutes(accessTokenMinutes).toSeconds()
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        if (request.getRefreshToken() == null
                || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Refresh token is required");
        }

        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        Instant now = Instant.now();

        if (storedToken.isRevoked() || !storedToken.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return createTokenResponse(storedToken.getUser(), now);
    }

    private TokenResponse createTokenResponse(User user, Instant now) {
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(accessTokenMinutes));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventra")
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .claim("role", user.getRole().name())
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String refreshTokenValue = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(hashToken(refreshTokenValue));
        newRefreshToken.setExpiresAt(now.plus(Duration.ofDays(7)));
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                Duration.ofMinutes(accessTokenMinutes).toSeconds()
        );
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }
}