package dev.a301.stock.repository.user;

import dev.a301.stock.entity.user.User;
import dev.a301.stock.dto.user.response.UserProfileResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

  boolean existsByNickname(String nickname);

  // socialEmail 필드 기준 조회
  Optional<User> findBySocialEmail(String socialEmail);

  @Query("""
    select new dev.a301.stock.dto.user.response.UserProfileResponse(
      u.userNo, u.nickname, u.socialEmail, u.createdAt, u.totalReward, u.cash
    )
    from User u
    where u.userNo = :userNo
  """)
  Optional<UserProfileResponse> findProfileByUserNo(@Param("userNo") Integer userNo);
}
