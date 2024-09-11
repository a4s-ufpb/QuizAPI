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
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q on r.question_id = q.id
            WHERE date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByDateTime(Pageable pageable, UUID uuid, LocalDate currentDate, LocalDate finalDate);

    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND r.date_time BETWEEN :currentDate AND :finalDate
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByDateTimeAndUserName(Pageable pageable, UUID uuid, String name, LocalDate currentDate, LocalDate finalDate);

    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q on r.question_id = q.id
            WHERE r.question_id = :questionId
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByQuestionId(Pageable pageable, UUID uuid, Long questionId);

    @Query(nativeQuery = true, value = """
            SELECT r.* FROM tb_response r
            JOIN tb_question q ON r.question_id = q.id
            JOIN tb_user u ON r.user_uuid = u.uuid
            WHERE u.name = :name
            AND q.creator_uuid = :uuid
            """)
    Page<Response> findByQuestionCreatorAndUserName(Pageable pageable, UUID uuid, String name);

    List<Response> findByQuestionCreatorAndQuestionThemeName(User creator, String themeName);
    List<Response> findByQuestionThemeName(String themeName);
}
