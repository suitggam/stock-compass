package dev.a301.stock.modules.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
  name = "oauth_identities",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_provider_subject", columnNames = {"provider","provider_user_id"})
  },
  indexes = { @Index(name = "idx_userno", columnList = "user_no") }
)
public class OauthIdentity {

  public enum Provider { GOOGLE, KAKAO }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_no", nullable = false, foreignKey = @ForeignKey(name = "fk_oauth_user"))
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 10)
  private Provider provider;

  @Column(name = "provider_user_id", nullable = false, length = 191)
  private String providerUserId;

  @Column(name = "provider_email", length = 254)
  private String providerEmail;

  @Column(name = "profile_image_url", length = 255)
  private String profileImageUrl;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Column(name = "connected_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime connectedAt;
}
