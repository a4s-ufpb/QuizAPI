package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Response;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response,Long> {
    Page<Response> findByUser(Pageable pageable, User user);

    // Respostas do próprio usuário (como jogador), com filtros opcionais de tema, intervalo de datas e modo de jogo
    @Query("SELECT r FROM tb_response r WHERE r.user = :user " +
            "AND r.gameMode = :gameMode " +
            "AND (:themeName = '' OR r.question.theme.name = :themeName) " +
            "AND (:startDate IS NULL OR r.dateTime >= :startDate) " +
            "AND (:endDate IS NULL OR r.dateTime <= :endDate)")
    Page<Response> findByUserAndFilters(Pageable pageable, @Param("user") User user,
                                         @Param("themeName") String themeName,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("gameMode") GameMode gameMode);

    @Query("SELECT COUNT(r) FROM tb_response r WHERE r.user = :user AND r.alternative.correct = :correct " +
            "AND r.gameMode = :gameMode " +
            "AND (:themeName = '' OR r.question.theme.name = :themeName) " +
            "AND (:startDate IS NULL OR r.dateTime >= :startDate) " +
            "AND (:endDate IS NULL OR r.dateTime <= :endDate)")
    long countByUserAndCorrectAndFilters(@Param("user") User user, @Param("correct") boolean correct,
                                          @Param("themeName") String themeName,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("gameMode") GameMode gameMode);
    Page<Response> findByQuestionCreator(Pageable pageable, User creator);
    Page<Response> findByQuestionCreatorAndGameMode(Pageable pageable, User creator, GameMode gameMode);
    List<Response> findByQuestionCreatorUuid(UUID creatorId);

    // Painel de respostas do criador: todos os filtros opcionais numa única query
    @Query("SELECT r FROM tb_response r WHERE r.gameMode = :gameMode " +
            "AND r.question.creator.uuid = :creatorUuid " +
            "AND (:name = '' OR r.user.name = :name) " +
            "AND (:themeName = '' OR r.question.theme.name = :themeName) " +
            "AND (:currentDate IS NULL OR r.dateTime >= :currentDate) " +
            "AND (:finalDate IS NULL OR r.dateTime <= :finalDate)")
    Page<Response> findByCreatorAndFilters(Pageable pageable,
                                           @Param("creatorUuid") UUID creatorUuid,
                                           @Param("name") String name,
                                           @Param("themeName") String themeName,
                                           @Param("currentDate") LocalDate currentDate,
                                           @Param("finalDate") LocalDate finalDate,
                                           @Param("gameMode") GameMode gameMode);

    // Mesma busca sem restrição de criador — usada pelo ADMIN, que enxerga todas as respostas
    @Query("SELECT r FROM tb_response r WHERE r.gameMode = :gameMode " +
            "AND (:name = '' OR r.user.name = :name) " +
            "AND (:themeName = '' OR r.question.theme.name = :themeName) " +
            "AND (:currentDate IS NULL OR r.dateTime >= :currentDate) " +
            "AND (:finalDate IS NULL OR r.dateTime <= :finalDate)")
    Page<Response> findByFiltersForAdmin(Pageable pageable,
                                         @Param("name") String name,
                                         @Param("themeName") String themeName,
                                         @Param("currentDate") LocalDate currentDate,
                                         @Param("finalDate") LocalDate finalDate,
                                         @Param("gameMode") GameMode gameMode);

    // Busca por intervalo de datas
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q on r.question_id = q.id
            WHERE date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByDateTime(Pageable pageable, UUID uuid, LocalDate currentDate, LocalDate finalDate, String gameMode);

    // Busca por nome de usuário
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND q.creator_uuid = :uuid
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByQuestionCreatorAndUserName(Pageable pageable, UUID uuid, String name, String gameMode);

    // Busca por nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_theme t ON q.theme_id = t.id
            AND q.creator_uuid = :uuid
            AND t.name = :themeName
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByQuestionCreatorAndThemeName(Pageable pageable, UUID uuid, String themeName, String gameMode);

    // Busca pelo nome de usuário e nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            JOIN tb_theme t ON q.theme_id = t.id
            WHERE u.name = :name
            AND q.creator_uuid = :uuid
            AND t.name = :themeName
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByQuestionCreatorUserNameAndThemeName(Pageable pageable, UUID uuid, String name, String themeName, String gameMode);

    // Busca por intervalo de datas e nome de usuário
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByDateTimeAndUserName(Pageable pageable, UUID uuid, String name, LocalDate currentDate, LocalDate finalDate, String gameMode);

    // Busca por intervalo de datas e nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_theme t ON q.theme_id = t.id
            WHERE t.name = :themeName
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByDateTimeAndThemeName(Pageable pageable, UUID uuid, String themeName, LocalDate currentDate, LocalDate finalDate, String gameMode);

    // Busca por intervalo de datas, nome de usuário e nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            JOIN tb_theme t ON q.theme_id = t.id
            WHERE u.name = :name
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            AND t.name = :themeName
            AND r.game_mode = :gameMode
            """)
    Page<Response> findByDateTimeAndUserNameAndThemeName(Pageable pageable, UUID uuid, String name, String themeName, LocalDate currentDate, LocalDate finalDate, String gameMode);

    List<Response> findByQuestionCreatorAndQuestionThemeName(User creator, String themeName);
    List<Response> findByQuestionThemeName(String themeName);
    List<Response> findByQuestionCreatorAndQuestionThemeNameAndGameMode(User creator, String themeName, GameMode gameMode);
    List<Response> findByQuestionThemeNameAndGameMode(String themeName, GameMode gameMode);
}
