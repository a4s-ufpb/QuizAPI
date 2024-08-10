package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question,Long> {
    Page<Question> findByThemeId(Long id, Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT * FROM tb_question
            WHERE theme_id = :idTheme
            ORDER BY RANDOM()
            LIMIT 10; 
            """)
    List<Question> find10QuestionsByThemeId(Long idTheme);

    List<Question> findByThemeId(Long idTheme);

    @Query(nativeQuery = true, value = """
            SELECT * FROM tb_question
            WHERE theme_id = :idTheme
            AND creator_id = :idCreator
            ORDER BY RANDOM()
            LIMIT 10;
            """)
    List<Question> find10QuestionsByThemeIdAndCreatorId(Long idTheme, UUID idCreator);

    Page<Question> findByCreatorAndThemeId(User user, Long id, Pageable pageable);
    Page<Question> findByCreatorAndThemeIdAndTitleStartsWithIgnoreCase(User creator, Long themeId, String title, Pageable pageable);
}
