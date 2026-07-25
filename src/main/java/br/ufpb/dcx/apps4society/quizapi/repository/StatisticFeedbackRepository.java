package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.StatisticFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StatisticFeedbackRepository extends JpaRepository<StatisticFeedback, Long> {
    List<StatisticFeedback> findByStatistic_IdOrderByCreatedAtDesc(Long statisticId);

    @Query("select f from tb_statistic_feedback f where f.statistic.studentUuid = :studentUuid order by f.createdAt desc")
    List<StatisticFeedback> findByStudentUuid(UUID studentUuid);

    List<StatisticFeedback> findByCreator_UuidOrderByCreatedAtDesc(UUID creatorUuid);
}
