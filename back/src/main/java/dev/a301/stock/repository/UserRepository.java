package dev.a301.stock.repository;

import dev.a301.stock.domain.User;
import dev.a301.stock.web.dto.LoginUserDto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // ✅ 이메일로 사용자 찾기
    Optional<User> findBySocialEmail(String socialEmail);

    @Query("""
      select new dev.a301.stock.web.dto.LoginUserDto(
        u.userNo, u.socialEmail, u.nickname, u.cancel, u.createdAt,
        u.top1, u.top2, u.top3, u.topten, u.asset, u.cash
      )
      from User u
      where u.userNo = :id
      """)
    Optional<LoginUserDto> findLoginUserDtoById(@Param("id") Integer id);
}