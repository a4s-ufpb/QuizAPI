package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.ThemeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThemeLikeRepository extends JpaRepository<ThemeLike, Long> {
    Optional<ThemeLike> findByUser_UuidAndTheme_Id(UUID userUuid, Long themeId);
    long countByTheme_Id(Long themeId);
    boolean existsByUser_UuidAndTheme_Id(UUID userUuid, Long themeId);

    @Query("select tl.theme.id from tb_theme_like tl where tl.user.uuid = :userUuid and tl.theme.id in :themeIds")
    List<Long> findLikedThemeIds(UUID userUuid, List<Long> themeIds);
}
