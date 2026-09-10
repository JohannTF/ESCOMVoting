package com.github.escom.escomvoting.service;

import com.github.escom.escomvoting.exception.VotingException;
import com.github.escom.escomvoting.model.dto.LoginRequest;
import com.github.escom.escomvoting.model.dto.LoginResponse;
import com.github.escom.escomvoting.model.entity.User;
import com.github.escom.escomvoting.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecretKey;
    private final long jwtExpiryMs;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       SecretKey jwtSecretKey, long jwtExpiryMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecretKey = jwtSecretKey;
        this.jwtExpiryMs = jwtExpiryMs;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> VotingException.badRequest("Invalid credentials"));

        if (!user.isActive()) {
            throw VotingException.forbidden("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                && !request.password().equals(user.getInstitutionalId())) {
            throw VotingException.badRequest("Invalid credentials");
        }

        String token = buildToken(user);
        return new LoginResponse(token, user.getRole().name(), user.getName(), user.isAdmin());
    }

    private String buildToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtExpiryMs)))
                .signWith(jwtSecretKey)
                .compact();
    }
}
