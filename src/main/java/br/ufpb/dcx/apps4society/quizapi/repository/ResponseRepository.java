package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Response;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response,Long> {
    Page<Response> findByUser(Pageable pageable, User user);
    Page<Response> findByQuestionCreator(Pageable pageable, User creator);
    List<Response> findByQuestionCreatorUuid(UUID creatorId);

    // Busca por intervalo de datas
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q on r.question_id = q.id
            WHERE date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByDateTime(Pageable pageable, UUID uuid, LocalDate currentDate, LocalDate finalDate);

    // Busca por nome de usuário
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByQuestionCreatorAndUserName(Pageable pageable, UUID uuid, String name);

    // Busca por nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_theme t ON q.theme_id = t.id
            AND q.creator_uuid = :uuid
            AND t.name = :themeName
            """)
    Page<Response> findByQuestionCreatorAndThemeName(Pageable pageable, UUID uuid, String themeName);

    // Busca pelo nome de usuário e nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            JOIN tb_theme t ON q.theme_id = t.id
            WHERE u.name = :name
            AND q.creator_uuid = :uuid
            AND t.name = :themeName
            """)
    Page<Response> findByQuestionCreatorUserNameAndThemeName(Pageable pageable, UUID uuid, String name, String themeName);

    // Busca por intervalo de datas e nome de usuário
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByDateTimeAndUserName(Pageable pageable, UUID uuid, String name, LocalDate currentDate, LocalDate finalDate);

    // Busca por intervalo de datas e nome do tema
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_theme t ON q.theme_id = t.id
            WHERE t.name = :themeName
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByDateTimeAndThemeName(Pageable pageable, UUID uuid, String themeName, LocalDate currentDate, LocalDate finalDate);

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
            """)
    Page<Response> findByDateTimeAndUserNameAndThemeName(Pageable pageable, UUID uuid, String name, String themeName, LocalDate currentDate, LocalDate finalDate);

    List<Response> findByQuestionCreatorAndQuestionThemeName(User creator, String themeName);
    List<Response> findByQuestionThemeName(String themeName);
}
