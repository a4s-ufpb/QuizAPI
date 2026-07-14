package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StatisticRepository extends JpaRepository<StatisticPerConclusion, Long> {
    Page<StatisticPerConclusion> findByCreatorId(Pageable pageable, UUID creatorId);
    List<StatisticPerConclusion> findByCreatorId(UUID creatorId);
    Page<StatisticPerConclusion> findByCreatorIdAndStudentName(Pageable pageable, UUID creatorId, String studentName);
    Page<StatisticPerConclusion> findByCreatorIdAndThemeName(Pageable pageable, UUID creatorId, String themeName);
    // 2. Buscar por creatorId e intervalo de datas
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId AND s.date BETWEEN :startDate AND :endDate")
    Page<StatisticPerConclusion> findByCreatorIdAndDateRange(
            Pageable pageable,
            @Param("creatorId") UUID creatorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 2. Buscar por creatorId, studentName e intervalo de datas
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId AND s.studentName = :studentName AND s.date BETWEEN :startDate AND :endDate")
    Page<StatisticPerConclusion> findByCreatorIdAndStudentNameAndDateRange(
            Pageable pageable,
            @Param("creatorId") UUID creatorId,
            @Param("studentName") String studentName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 3. Buscar por creatorId, themeName e intervalo de datas
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId AND s.themeName = :themeName AND s.date BETWEEN :startDate AND :endDate")
    Page<StatisticPerConclusion> findByCreatorIdAndThemeNameAndDateRange(
            Pageable pageable,
            @Param("creatorId") UUID creatorId,
            @Param("themeName") String themeName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 4. Buscar por creatorId, themeName, studentName e intervalo de datas
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId AND s.themeName = :themeName AND s.studentName = :studentName AND s.date BETWEEN :startDate AND :endDate")
    Page<StatisticPerConclusion> findByCreatorIdAndThemeNameAndStudentNameAndDateRange(
            Pageable pageable,
            @Param("creatorId") UUID creatorId,
            @Param("themeName") String themeName,
            @Param("studentName") String studentName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId AND s.themeName = :themeName AND s.studentName = :studentName")
    Page<StatisticPerConclusion> findByCreatorIdAndThemeNameAndStudentName(
            Pageable pageable,
            @Param("creatorId") UUID creatorId,
            @Param("themeName") String themeName,
            @Param("studentName") String studentName
    );

    // Estatísticas do criador com todos os filtros opcionais numa única query
    @Query("SELECT s FROM tb_statistic s WHERE s.creatorId = :creatorId " +
            "AND (:studentName = '' OR s.studentName = :studentName) " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND (:startDate IS NULL OR s.date >= :startDate) " +
            "AND (:endDate IS NULL OR s.date <= :endDate)")
    Page<StatisticPerConclusion> findByCreatorIdAndFilters(Pageable pageable,
                                                           @Param("creatorId") UUID creatorId,
                                                           @Param("studentName") String studentName,
                                                           @Param("themeName") String themeName,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    // Mesma busca sem restrição de criador — usada pelo ADMIN, que enxerga todas as estatísticas
    @Query("SELECT s FROM tb_statistic s WHERE (:studentName = '' OR s.studentName = :studentName) " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND (:startDate IS NULL OR s.date >= :startDate) " +
            "AND (:endDate IS NULL OR s.date <= :endDate)")
    Page<StatisticPerConclusion> findByFiltersForAdmin(Pageable pageable,
                                                       @Param("studentName") String studentName,
                                                       @Param("themeName") String themeName,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    // Total de quizzes concluídos pelo próprio usuário (como aluno), com filtros opcionais
    @Query("SELECT COUNT(s) FROM tb_statistic s WHERE s.studentName = :studentName " +
            "AND (:themeName = '' OR s.themeName = :themeName) " +
            "AND (:startDate IS NULL OR s.date >= :startDate) " +
            "AND (:endDate IS NULL OR s.date <= :endDate)")
    long countByStudentNameAndFilters(@Param("studentName") String studentName,
                                       @Param("themeName") String themeName,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
