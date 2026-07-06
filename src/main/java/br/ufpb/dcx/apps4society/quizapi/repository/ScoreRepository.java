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

    // Duas queries separadas (em vez de "WHERE :since IS NULL OR ...") porque o
    // Postgres não consegue inferir o tipo do parâmetro quando ele só aparece
    // num "IS NULL" — gera "could not determine data type of parameter $1".
    @Query("""
            SELECT s.user as user, SUM(s.finalResult) as total FROM tb_score s
            GROUP BY s.user
            ORDER BY SUM(s.finalResult) DESC
            """)
    Page<GlobalRankingProjection> findGlobalRankingAll(Pageable pageable);

    @Query("""
            SELECT s.user as user, SUM(s.finalResult) as total FROM tb_score s
            WHERE s.createdAt >= :since
            GROUP BY s.user
            ORDER BY SUM(s.finalResult) DESC
            """)
    Page<GlobalRankingProjection> findGlobalRankingSince(@Param("since") LocalDateTime since, Pageable pageable);
}
