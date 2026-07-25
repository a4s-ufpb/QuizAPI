package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Theme findByNameIgnoreCase(String name);
    Page<Theme> findByNameStartsWithIgnoreCase(String name, Pageable pageable);
    Page<Theme> findByCreator(User creator, Pageable pageable);
    Page<Theme> findByCreatorAndNameStartsWithIgnoreCase(User creator, String name, Pageable pageable);

    @Query("select distinct t from tb_theme t join t.tags tag where tag.id in :tagIds " +
           "and (:name = '' or lower(t.name) like lower(concat(:name, '%')))")
    Page<Theme> findByTagIds(@Param("tagIds") List<Long> tagIds, @Param("name") String name, Pageable pageable);
}
