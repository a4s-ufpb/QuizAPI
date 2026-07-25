package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.Tag;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByCreatorOrderByNameAsc(User creator);
    Tag findByCreatorAndNameIgnoreCase(User creator, String name);
    List<Tag> findByIdIn(List<Long> ids);
    List<Tag> findByCreator_UuidOrderByNameAsc(UUID creatorUuid);
}
