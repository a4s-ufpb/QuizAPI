package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    @Query(nativeQuery = true, value = """
            SELECT * FROM tb_score
            WHERE theme_id = :themeId
            ORDER BY result DESC;
            """)
    List<Score> findByTheme(Long themeId);

    @Query("""
            SELECT s.user as user, SUM(s.finalResult) as total FROM tb_score s
            WHERE (:since IS NULL OR s.createdAt >= :since)
            GROUP BY s.user
            ORDER BY SUM(s.finalResult) DESC
            """)
    Page<GlobalRankingProjection> findGlobalRanking(@Param("since") LocalDateTime since, Pageable pageable);
}
