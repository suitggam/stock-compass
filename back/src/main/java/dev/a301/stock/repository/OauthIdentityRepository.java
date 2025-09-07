package dev.a301.stock.repository;

import dev.a301.stock.domain.OauthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {
  Optional<OauthIdentity> findByProviderAndProviderUserId(
      OauthIdentity.Provider provider, String providerUserId);
}