package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.WalletTransaction;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {
    @Mock WalletTransactionRepository walletTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @InjectMocks WalletService walletService;

    User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "User", "u@u.com", "12345678", Role.USER);
    }

    @Test
    void earn_positiveAmount_addsCoinsAndRecordsTransaction() {
        walletService.earn(user, 100, "Partida");

        assertEquals(100, user.getCoins());
        verify(userRepository).save(user);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void earn_zeroOrNegative_isNoOp() {
        walletService.earn(user, 0, "nada");

        assertEquals(0, user.getCoins());
        verify(userRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void spend_sufficientBalance_returnsTrueAndDebits() {
        user.addCoins(200);

        boolean ok = walletService.spend(user, 150, "Compra");

        assertTrue(ok);
        assertEquals(50, user.getCoins());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void spend_insufficientBalance_returnsFalseAndDoesNotSave() {
        user.addCoins(50);

        boolean ok = walletService.spend(user, 150, "Compra");

        assertFalse(ok);
        assertEquals(50, user.getCoins());
        verify(userRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void findMyWallet_returnsCurrentBalance() {
        user.addCoins(80);
        when(userService.findUserByToken("token")).thenReturn(user);

        WalletResponse response = walletService.findMyWallet("token");

        assertEquals(80, response.coins());
    }
}
