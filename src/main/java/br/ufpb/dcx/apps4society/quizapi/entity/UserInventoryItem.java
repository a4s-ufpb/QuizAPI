package br.ufpb.dcx.apps4society.quizapi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "tb_user_inventory_item")
public class UserInventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    private String itemCode;
    private LocalDateTime purchasedAt = LocalDateTime.now();

    public UserInventoryItem() {}

    public UserInventoryItem(User user, String itemCode) {
        this.user = user;
        this.itemCode = itemCode;
    }

    public String getItemCode() {
        return itemCode;
    }
}
