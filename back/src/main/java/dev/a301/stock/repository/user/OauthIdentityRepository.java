package dev.a301.stock.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.a301.stock.entity.user.OauthIdentity;
import dev.a301.stock.entity.user.OauthIdentity.Provider;

import java.util.Optional;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {
  Optional<OauthIdentity> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
