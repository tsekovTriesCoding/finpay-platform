package com.finpay.user.repository;

import com.finpay.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByStatus(User.UserStatus status);

    List<User> findByRole(User.UserRole role);

    /**
     * Search users by name or email, excluding the current user.
     * Matches against firstName, lastName, or email containing the search term.
     */
    @Query("SELECT u FROM User u WHERE u.id != :excludeUserId AND u.status = 'ACTIVE' AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, 
                           @Param("excludeUserId") UUID excludeUserId, 
                           Pageable pageable);

    // Admin query methods

    /** Paginated role query for admin user listing */
    Page<User> findByRole(User.UserRole role, Pageable pageable);

    /** Paginated status query for admin user listing */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /** Paginated role + status combo filter */
    Page<User> findByRoleAndStatus(User.UserRole role, User.UserStatus status, Pageable pageable);

    /**
     * Admin search: find users by name or email (no exclusion, all statuses).
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> adminSearchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /** Count users by status */
    long countByStatus(User.UserStatus status);

    /** Count users by role */
    long countByRole(User.UserRole role);
}
