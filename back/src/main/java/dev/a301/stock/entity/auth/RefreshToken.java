package dev.a301.stock.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import dev.a301.stock.entity.user.User;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
  @Index(name = "idx_rt_user", columnList = "user_no")
})
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_no", nullable = false,
    foreignKey = @ForeignKey(name = "fk_rt_user"))
  private User user;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked", nullable = false)
  private Boolean revoked = false;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "ip", length = 45)
  private String ip;
}
