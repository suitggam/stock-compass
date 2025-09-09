package dev.a301.stock.modules.user.service;

import dev.a301.stock.modules.user.domain.OauthIdentity;
import dev.a301.stock.modules.user.domain.User;
import dev.a301.stock.modules.user.repository.OauthIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OauthIdentityService {
  private final OauthIdentityRepository repository;

  @Transactional(readOnly = true)
  public OauthIdentity find(OauthIdentity.Provider provider, String providerUserId) {
    return repository.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);
  }

  @Transactional
  public OauthIdentity link(User user,
                            OauthIdentity.Provider provider,
                            String providerUserId,
                            String providerEmail,
                            String profileImageUrl,
                            boolean emailVerified) {
    OauthIdentity id = repository
      .findByProviderAndProviderUserId(provider, providerUserId)
      .orElseGet(OauthIdentity::new);

    id.setUser(user);
    id.setProvider(provider);
    id.setProviderUserId(providerUserId);
    id.setProviderEmail(providerEmail);
    id.setProfileImageUrl(profileImageUrl);
    id.setEmailVerified(emailVerified);
    return repository.save(id);
  }
}
