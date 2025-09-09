package dev.a301.stock.modules.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.a301.stock.modules.user.domain.OauthIdentity;

import java.util.Optional;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {
  Optional<OauthIdentity> findByProviderAndProviderUserId(
      OauthIdentity.Provider provider, String providerUserId);
}