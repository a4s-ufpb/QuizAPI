package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.ThemeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeFeedbackRepository extends JpaRepository<ThemeFeedback, Long> {
    List<ThemeFeedback> findByTheme_IdOrderByCreatedAtDesc(Long themeId);
    long countByTheme_Id(Long themeId);
}
