package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.config.JwtService;
import org.aleksanyan.medserver.domain.Token;
import org.aleksanyan.medserver.domain.User;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-ttl}")
    private java.time.Duration refreshTtl;

    @Transactional
    public String issueRefreshToken(User user) {
        String refresh = jwtService.generateRefresh(user.getEmail());

        Token token = Token.builder()
                .user(user)
                .refreshToken(refresh)
                .expiresAt(OffsetDateTime.now().plus(refreshTtl))
                .revoked(false)
                .build();

        tokenRepository.save(token);
        return refresh;
    }

    public Token validateRefreshToken(String refreshToken) {
        var token = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isRevoked() || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        if (!jwtService.isValid(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return token;
    }

    @Transactional
    public void revokeTokens(User user) {
        tokenRepository.deleteAllByUser(user);
    }
}
