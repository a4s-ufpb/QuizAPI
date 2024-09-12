package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatisticRepository extends JpaRepository<StatisticPerConclusion, Long> {
    Page<StatisticPerConclusion> findByCreatorId(Pageable pageable, UUID creatorId);
    Page<StatisticPerConclusion> findByCreatorIdAndStudentName(Pageable pageable, UUID creatorId, String studentName);
    Page<StatisticPerConclusion> findByCreatorIdAndThemeName(Pageable pageable, UUID creatorId, String themeName);
    Page<StatisticPerConclusion> findByCreatorIdAndStudentNameAndThemeName(Pageable pageable, UUID creatorId, String studentName, String themeName);
}
