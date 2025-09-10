package dev.a301.stock.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import dev.a301.stock.entity.user.User;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "favorite_stocks",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_stock", columnNames = {"user_no","stock_pk"})
  },
  indexes = { @Index(name = "idx_fav_user", columnList = "user_no") }
)
public class FavoriteStock {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "interest_no")
  private Long interestNo;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_no", nullable = false,
    foreignKey = @ForeignKey(name = "fk_fav_user"))
  private User user;

  @Column(name = "stock_pk", nullable = false)
  private Long stockPk;

  // 스키마 컬럼명이 대문자이므로 명시적으로 매핑
  @Column(name = "ISCANCEL", nullable = false)
  private Boolean isCancel = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
