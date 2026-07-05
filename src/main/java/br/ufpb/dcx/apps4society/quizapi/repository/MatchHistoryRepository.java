package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.MatchHistory;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchHistoryRepository extends JpaRepository<MatchHistory, Long> {
    Page<MatchHistory> findByUserOrderByPlayedAtDesc(User user, Pageable pageable);
    long countByUser(User user);

    @Query("SELECT COUNT(m) FROM tb_match_history m WHERE m.user = :user AND m.score = m.total AND m.total > 0")
    long countPerfectMatches(@Param("user") User user);
}
