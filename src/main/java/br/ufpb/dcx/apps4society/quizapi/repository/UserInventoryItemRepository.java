package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.UserInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInventoryItemRepository extends JpaRepository<UserInventoryItem, Long> {
    List<UserInventoryItem> findByUser(User user);
    boolean existsByUserAndItemCode(User user, String itemCode);
}
