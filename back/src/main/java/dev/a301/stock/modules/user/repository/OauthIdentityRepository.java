package dev.a301.stock.modules.user.repository;

import dev.a301.stock.modules.user.domain.OauthIdentity;
import dev.a301.stock.modules.user.domain.OauthIdentity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {
  Optional<OauthIdentity> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
