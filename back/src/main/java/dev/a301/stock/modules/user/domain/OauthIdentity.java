package dev.a301.stock.modules.user.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_identities",
       uniqueConstraints = @UniqueConstraint(name="uq_provider_subject",
         columnNames = {"provider","provider_user_id"}))
@Getter @Setter @NoArgsConstructor
public class OauthIdentity {
  public enum Provider { GOOGLE, KAKAO }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_no")
  private User user;

  @Enumerated(EnumType.STRING)
  private Provider provider;

  @Column(name="provider_user_id", nullable = false, length = 100)
  private String providerUserId;

  private String providerEmail;
  private String profileImageUrl;
  private boolean emailVerified;

  private LocalDateTime connectedAt = LocalDateTime.now();
}