package dev.a301.stock.modules.user.service;

import dev.a301.stock.modules.user.domain.OauthIdentity;
import dev.a301.stock.modules.user.domain.User;
import dev.a301.stock.modules.user.repository.OauthIdentityRepository;
import dev.a301.stock.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OauthIdentityService {
  private final OauthIdentityRepository idRepo;
  private final UserRepository userRepo;

  @Transactional
  public Integer upsert(String providerStr, String providerUserId,
                        String email, String nickname, String imageUrl,
                        boolean emailVerified) {
    var provider = OauthIdentity.Provider.valueOf(providerStr.toUpperCase());

    var existing = idRepo.findByProviderAndProviderUserId(provider, providerUserId);
    if (existing.isPresent()) {
      var u = existing.get().getUser();
      u.setLastLoginAt(LocalDateTime.now());
      return u.getUserNo();
    }

    User user = null;
    if (email != null) user = userRepo.findBySocialEmail(email).orElse(null);

    if (user == null) {
      user = new User();
      user.setSocialEmail(email);
      user.setNickname(genNickname(nickname, email));
      user.setCreatedAt(LocalDateTime.now());
      user.setLastLoginAt(LocalDateTime.now());
      userRepo.save(user);
    } else {
      user.setLastLoginAt(LocalDateTime.now());
    }

    var link = new OauthIdentity();
    link.setUser(user);
    link.setProvider(provider);
    link.setProviderUserId(providerUserId);
    link.setProviderEmail(email);
    link.setProfileImageUrl(imageUrl);
    link.setEmailVerified(emailVerified);
    idRepo.save(link);

    return user.getUserNo();
  }

  private String genNickname(String base, String email) {
    String seed = base != null ? base : (email != null ? email.split("@")[0] : "user");
    return seed.replaceAll("\\W+","") + "_" + (1000 + new Random().nextInt(9000));
  }
}