package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Friendship;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
            SELECT f FROM tb_friendship f
            WHERE (f.requester = :a AND f.addressee = :b)
               OR (f.requester = :b AND f.addressee = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") User a, @Param("b") User b);

    @Query("""
            SELECT f FROM tb_friendship f
            WHERE (f.requester = :user OR f.addressee = :user) AND f.status = :status
            """)
    List<Friendship> findAllByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);

    @Query("""
            SELECT f FROM tb_friendship f
            WHERE f.addressee = :user AND f.status = 'PENDING'
            """)
    List<Friendship> findPendingForAddressee(@Param("user") User user);
}
