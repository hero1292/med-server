package org.aleksanyan.medserver.repo;

import org.aleksanyan.medserver.domain.Token;
import org.aleksanyan.medserver.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByRefreshToken(String refreshToken);
    void deleteAllByUser(User user);
}
