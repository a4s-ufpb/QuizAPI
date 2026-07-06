package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletTransactionResponse;
import br.ufpb.dcx.apps4society.quizapi.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> findMyWallet(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(walletService.findMyWallet(token));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> findMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String token) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(walletService.findMyTransactions(token, pageable));
    }
}
