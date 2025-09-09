package dev.a301.stock.modules.user.web.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginUserDto {
  private Integer userNo;
  private String socialEmail;   
  private String nickname;

  private String accessToken;
  private String refreshToken;
}
