package com.cyberrange.pointsmall.repository;

import com.cyberrange.pointsmall.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.availablePoints = u.availablePoints + :points, u.totalPoints = u.totalPoints + :points WHERE u.id = :userId")
    int addPoints(@Param("userId") Long userId, @Param("points") Long points);

    @Modifying
    @Query("UPDATE User u SET u.availablePoints = u.availablePoints - :points WHERE u.id = :userId AND u.availablePoints >= :points")
    int deductPoints(@Param("userId") Long userId, @Param("points") Long points);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    java.util.List<User> searchUsers(@Param("keyword") String keyword);
}
