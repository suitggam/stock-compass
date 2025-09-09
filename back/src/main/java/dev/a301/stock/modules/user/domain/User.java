package dev.a301.stock.modules.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
  name = "users",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "social_email"),
    @UniqueConstraint(name = "uq_users_nickname", columnNames = "nickname")
  },
  indexes = { @Index(name = "idx_users_email", columnList = "social_email") }
)
@DynamicInsert @DynamicUpdate
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_no")
  private Integer userNo;

  @Column(name = "social_email", nullable = false, length = 254)
  private String socialEmail;

  @Column(name = "nickname", nullable = false, length = 30)
  private String nickname;

  @Column(name = "cancel", nullable = false)
  private Boolean cancel = false;     // TINYINT(1) ↔ Boolean

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  @Column(name = "total_reward", nullable = false)
  private Integer totalReward;        // DB default 사용

  @Column(name = "cash", nullable = false)
  private Integer cash;               // DB default 사용
}
