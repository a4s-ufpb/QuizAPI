package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StatisticRepository extends JpaRepository<StatisticPerConclusion, Long> {
    Page<StatisticPerConclusion> findByCreatorId(Pageable pageable, UUID creatorId);
    List<StatisticPerConclusion> findByCreatorId(UUID creatorId);

    // Estatísticas do criador com todos os filtros opcionais numa única query.
    // gameMode == null significa "todos os modos".
    //
    // Datas: `s.date` é LocalDateTime; o service converte o filtro (LocalDate)
    // em [startDateTime, endDateTimeExclusive) e usa sentinelas quando ausente
    // — parâmetro null de data faz o driver Postgres não inferir o tipo.
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId " +
            "AND (:studentName = '' OR s.studentName = :studentName) " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND s.date >= :startDateTime " +
            "AND s.date < :endDateTimeExclusive " +
            "AND (:gameMode IS NULL OR s.gameMode = :gameMode)")
    Page<StatisticPerConclusion> findByCreatorIdAndFilters(Pageable pageable,
                                                           @Param("creatorId") UUID creatorId,
                                                           @Param("studentName") String studentName,
                                                           @Param("themeName") String themeName,
                                                           @Param("startDateTime") LocalDateTime startDateTime,
                                                           @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                                                           @Param("gameMode") GameMode gameMode);

    @Query("SELECT s FROM tb_statistic s WHERE (:studentName = '' OR s.studentName = :studentName) " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND s.date >= :startDateTime " +
            "AND s.date < :endDateTimeExclusive " +
            "AND (:gameMode IS NULL OR s.gameMode = :gameMode)")
    Page<StatisticPerConclusion> findByFiltersForAdmin(Pageable pageable,
                                                       @Param("studentName") String studentName,
                                                       @Param("themeName") String themeName,
                                                       @Param("startDateTime") LocalDateTime startDateTime,
                                                       @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                                                       @Param("gameMode") GameMode gameMode);

    @Query("SELECT COUNT(s) FROM tb_statistic s WHERE s.studentName = :studentName " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND s.date >= :startDateTime " +
            "AND s.date < :endDateTimeExclusive " +
            "AND (:gameMode IS NULL OR s.gameMode = :gameMode)")
    long countByStudentNameAndFilters(@Param("studentName") String studentName,
                                       @Param("themeName") String themeName,
                                       @Param("startDateTime") LocalDateTime startDateTime,
                                       @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                                       @Param("gameMode") GameMode gameMode);
}
