package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    @Query(nativeQuery = true, value = """
            SELECT * FROM tb_score
            WHERE theme_id = :themeId
            ORDER BY result DESC;
            """)
    List<Score> findByTheme(Long themeId);
}
