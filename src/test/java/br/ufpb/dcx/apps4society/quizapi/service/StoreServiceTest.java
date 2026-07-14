package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.store.StoreItemResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.UserInventoryItem;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.UserInventoryItemRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StoreServiceTest {
    @Mock UserInventoryItemRepository inventoryRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock WalletService walletService;
    @InjectMocks StoreService storeService;

    User user;
    // Item real do catálogo (StoreService.CATALOG).
    static final String ITEM = "TITLE_ROOKIE";
    static final int ITEM_PRICE = 80;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "User", "u@u.com", "12345678", Role.USER);
        when(userService.findUserByToken("token")).thenReturn(user);
    }

    @Test
    void findCatalog_marksOwnedItems() {
        when(inventoryRepository.findByUser(user))
                .thenReturn(List.of(new UserInventoryItem(user, ITEM)));

        List<StoreItemResponse> catalog = storeService.findCatalog("token");

        assertFalse(catalog.isEmpty());
        assertTrue(catalog.stream().anyMatch(i -> i.code().equals(ITEM) && i.owned()));
        assertTrue(catalog.stream().anyMatch(i -> !i.owned()));
    }

    @Test
    void purchase_notOwnedAndEnoughCoins_succeeds() {
        when(inventoryRepository.existsByUserAndItemCode(user, ITEM)).thenReturn(false);
        when(walletService.spend(eq(user), eq(ITEM_PRICE), anyString())).thenReturn(true);

        StoreItemResponse response = storeService.purchase(ITEM, "token");

        assertTrue(response.owned());
        verify(inventoryRepository).save(any(UserInventoryItem.class));
    }

    @Test
    void purchase_alreadyOwned_throws() {
        when(inventoryRepository.existsByUserAndItemCode(user, ITEM)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> storeService.purchase(ITEM, "token"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void purchase_insufficientBalance_throws() {
        when(inventoryRepository.existsByUserAndItemCode(user, ITEM)).thenReturn(false);
        when(walletService.spend(eq(user), eq(ITEM_PRICE), anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> storeService.purchase(ITEM, "token"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void purchase_itemNotInCatalog_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> storeService.purchase("NAO_EXISTE", "token"));
    }

    @Test
    void equip_ownedItem_setsSlot() {
        when(inventoryRepository.existsByUserAndItemCode(user, ITEM)).thenReturn(true);

        UserResponse response = storeService.equip(ITEM, "token");

        assertNotNull(response);
        assertEquals(ITEM, user.getEquippedTitle());
        verify(userRepository).save(user);
    }

    @Test
    void equip_notOwned_throws() {
        when(inventoryRepository.existsByUserAndItemCode(user, ITEM)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> storeService.equip(ITEM, "token"));
    }

    @Test
    void unequip_clearsSlot() {
        user.setEquippedTitle(ITEM);

        storeService.unequip("TITLE", "token");

        assertNull(user.getEquippedTitle());
        verify(userRepository).save(user);
    }
}
