package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.store.StoreItemResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.UserInventoryItem;
import br.ufpb.dcx.apps4society.quizapi.repository.UserInventoryItemRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo fixo (em código, sem tabela própria) de itens cosméticos
 * compráveis com moedas — títulos exibidos ao lado do nome e molduras de
 * avatar. Puramente estético, não afeta nenhuma regra de jogo.
 */
@Service
public class StoreService {
    public record CatalogItem(String code, String name, String description, String category, int price) {}

    // Categorias: TITLE (título ao lado do nome), AVATAR_FRAME (moldura do
    // avatar) e BANNER (faixa de fundo). O visual de cada código é resolvido
    // no frontend (util/cosmetics.ts) via CSS — aqui só o catálogo/preço.
    private static final List<CatalogItem> CATALOG = List.of(
            // Títulos
            new CatalogItem("TITLE_CHAMPION", "Campeão", "Título exibido ao lado do seu nome", "TITLE", 200),
            new CatalogItem("TITLE_LEGEND", "Lenda", "Título exibido ao lado do seu nome", "TITLE", 500),
            new CatalogItem("TITLE_MASTER", "Mestre do Quiz", "Título exibido ao lado do seu nome", "TITLE", 350),
            new CatalogItem("TITLE_ROOKIE", "Novato", "Título exibido ao lado do seu nome", "TITLE", 80),
            new CatalogItem("TITLE_SCHOLAR", "Erudito", "Título exibido ao lado do seu nome", "TITLE", 300),
            new CatalogItem("TITLE_PRODIGY", "Prodígio", "Título dourado brilhante", "TITLE", 450),
            new CatalogItem("TITLE_IMMORTAL", "Imortal", "Título lendário animado", "TITLE", 800),
            // Molduras de avatar
            new CatalogItem("FRAME_GOLD", "Moldura Dourada", "Borda dourada no seu avatar", "AVATAR_FRAME", 300),
            new CatalogItem("FRAME_NEON", "Moldura Neon", "Borda neon animada no seu avatar", "AVATAR_FRAME", 150),
            new CatalogItem("FRAME_FIRE", "Moldura de Fogo", "Borda em chamas no seu avatar", "AVATAR_FRAME", 400),
            new CatalogItem("FRAME_SILVER", "Moldura Prateada", "Borda prateada no seu avatar", "AVATAR_FRAME", 120),
            new CatalogItem("FRAME_RAINBOW", "Moldura Arco-íris", "Borda com gradiente animado", "AVATAR_FRAME", 550),
            new CatalogItem("FRAME_ICE", "Moldura de Gelo", "Borda azul-gelo cintilante", "AVATAR_FRAME", 350),
            // Banners (faixa de fundo)
            new CatalogItem("BANNER_OCEAN", "Banner Oceano", "Faixa de fundo azul do oceano", "BANNER", 200),
            new CatalogItem("BANNER_SUNSET", "Banner Pôr do Sol", "Faixa de fundo em tons quentes", "BANNER", 250),
            new CatalogItem("BANNER_FOREST", "Banner Floresta", "Faixa de fundo verde da floresta", "BANNER", 200),
            new CatalogItem("BANNER_GALAXY", "Banner Galáxia", "Faixa de fundo estrelada animada", "BANNER", 600),
            new CatalogItem("BANNER_AURORA", "Banner Aurora", "Faixa de fundo aurora boreal animada", "BANNER", 650)
    );

    private final UserInventoryItemRepository inventoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WalletService walletService;

    public StoreService(UserInventoryItemRepository inventoryRepository, UserRepository userRepository,
                        UserService userService, WalletService walletService) {
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.walletService = walletService;
    }

    public List<StoreItemResponse> findCatalog(String token) {
        User user = userService.findUserByToken(token);
        Set<String> owned = inventoryRepository.findByUser(user).stream()
                .map(UserInventoryItem::getItemCode)
                .collect(Collectors.toSet());

        return CATALOG.stream()
                .map(item -> new StoreItemResponse(
                        item.code(), item.name(), item.description(), item.category(),
                        item.price(), owned.contains(item.code())))
                .toList();
    }

    public List<String> findMyInventory(String token) {
        User user = userService.findUserByToken(token);
        return inventoryRepository.findByUser(user).stream().map(UserInventoryItem::getItemCode).toList();
    }

    public StoreItemResponse purchase(String itemCode, String token) {
        User user = userService.findUserByToken(token);
        CatalogItem item = CATALOG.stream()
                .filter(c -> c.code().equals(itemCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo"));

        if (inventoryRepository.existsByUserAndItemCode(user, itemCode)) {
            throw new IllegalArgumentException("Você já possui esse item");
        }

        boolean charged = walletService.spend(user, item.price(), "Compra: " + item.name());
        if (!charged) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        inventoryRepository.save(new UserInventoryItem(user, itemCode));
        return new StoreItemResponse(item.code(), item.name(), item.description(), item.category(), item.price(), true);
    }

    /** Equipa um item que o usuário já possui, no slot da categoria dele. */
    public UserResponse equip(String itemCode, String token) {
        User user = userService.findUserByToken(token);
        CatalogItem item = CATALOG.stream()
                .filter(c -> c.code().equals(itemCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo"));

        if (!inventoryRepository.existsByUserAndItemCode(user, itemCode)) {
            throw new IllegalArgumentException("Você não possui esse item");
        }

        applyToSlot(user, item.category(), itemCode);
        userRepository.save(user);
        return user.entityToResponse();
    }

    /** Desequipa (limpa) o slot de uma categoria. */
    public UserResponse unequip(String category, String token) {
        User user = userService.findUserByToken(token);
        applyToSlot(user, category.toUpperCase(), null);
        userRepository.save(user);
        return user.entityToResponse();
    }

    private void applyToSlot(User user, String category, String itemCode) {
        switch (category) {
            case "TITLE" -> user.setEquippedTitle(itemCode);
            case "AVATAR_FRAME" -> user.setEquippedFrame(itemCode);
            case "BANNER" -> user.setEquippedBanner(itemCode);
            default -> throw new IllegalArgumentException("Categoria inválida: " + category);
        }
    }
}
