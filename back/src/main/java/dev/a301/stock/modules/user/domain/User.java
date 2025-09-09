package dev.a301.stock.modules.user.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_no")
  private Integer userNo;

  private String socialEmail;

  @Column(nullable = false, length = 50)
  private String nickname;

  private boolean cancel;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime lastLoginAt;

  private Integer top1 = 0, top2 = 0, top3 = 0, topten = 0;
  private BigDecimal asset = BigDecimal.ZERO, cash = BigDecimal.ZERO;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OauthIdentity> identities = new ArrayList<>();
}