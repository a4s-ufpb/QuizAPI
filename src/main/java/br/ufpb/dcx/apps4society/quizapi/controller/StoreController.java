package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.store.StoreItemResponse;
import br.ufpb.dcx.apps4society.quizapi.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/store")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<StoreItemResponse>> findCatalog(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(storeService.findCatalog(token));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<String>> findMyInventory(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(storeService.findMyInventory(token));
    }

    @PostMapping("/purchase/{itemCode}")
    public ResponseEntity<?> purchase(@PathVariable String itemCode, @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(storeService.purchase(itemCode, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/equip/{itemCode}")
    public ResponseEntity<?> equip(@PathVariable String itemCode, @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(storeService.equip(itemCode, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/equip/{category}")
    public ResponseEntity<?> unequip(@PathVariable String category, @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(storeService.unequip(category, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
