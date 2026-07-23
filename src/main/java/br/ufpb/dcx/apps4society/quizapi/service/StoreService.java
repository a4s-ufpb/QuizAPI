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
    public record CatalogItem(String code, String name, String description, String category, int price, int requiredLevel) {}

    // Categorias: TITLE (título ao lado do nome), AVATAR_FRAME (moldura do
    // avatar) e BANNER (faixa de fundo). O visual de cada código é resolvido
    // no frontend (util/cosmetics.ts) via CSS — aqui só o catálogo/preço/nível.
    // requiredLevel: nível mínimo do usuário para poder comprar o item.
    private static final List<CatalogItem> CATALOG = List.of(
            // ---- Títulos ----
            new CatalogItem("TITLE_ROOKIE", "Novato", "Título exibido ao lado do seu nome", "TITLE", 80, 1),
            new CatalogItem("TITLE_APPRENTICE", "Aprendiz", "Título exibido ao lado do seu nome", "TITLE", 120, 2),
            new CatalogItem("TITLE_CHAMPION", "Campeão", "Título exibido ao lado do seu nome", "TITLE", 200, 3),
            new CatalogItem("TITLE_SCHOLAR", "Erudito", "Título exibido ao lado do seu nome", "TITLE", 300, 4),
            new CatalogItem("TITLE_STRATEGIST", "Estrategista", "Título exibido ao lado do seu nome", "TITLE", 320, 5),
            new CatalogItem("TITLE_MASTER", "Mestre do Quiz", "Título exibido ao lado do seu nome", "TITLE", 350, 6),
            new CatalogItem("TITLE_GENIUS", "Gênio", "Título com brilho ciano", "TITLE", 400, 7),
            new CatalogItem("TITLE_PRODIGY", "Prodígio", "Título dourado brilhante", "TITLE", 450, 8),
            new CatalogItem("TITLE_LEGEND", "Lenda", "Título exibido ao lado do seu nome", "TITLE", 500, 9),
            new CatalogItem("TITLE_MYTHIC", "Mítico", "Título com gradiente esmeralda animado", "TITLE", 700, 12),
            new CatalogItem("TITLE_IMMORTAL", "Imortal", "Título lendário animado", "TITLE", 800, 14),
            new CatalogItem("TITLE_GALACTIC", "Galáctico", "Título estelar animado", "TITLE", 950, 16),
            // ---- Molduras de avatar ----
            new CatalogItem("FRAME_BRONZE", "Moldura de Bronze", "Borda de bronze no seu avatar", "AVATAR_FRAME", 100, 1),
            new CatalogItem("FRAME_SILVER", "Moldura Prateada", "Borda prateada no seu avatar", "AVATAR_FRAME", 120, 2),
            new CatalogItem("FRAME_NEON", "Moldura Neon", "Borda neon animada no seu avatar", "AVATAR_FRAME", 150, 3),
            new CatalogItem("FRAME_GOLD", "Moldura Dourada", "Borda dourada no seu avatar", "AVATAR_FRAME", 300, 5),
            new CatalogItem("FRAME_EMERALD", "Moldura Esmeralda", "Borda verde-esmeralda cintilante", "AVATAR_FRAME", 330, 6),
            new CatalogItem("FRAME_ICE", "Moldura de Gelo", "Borda azul-gelo cintilante", "AVATAR_FRAME", 350, 7),
            new CatalogItem("FRAME_FIRE", "Moldura de Fogo", "Borda em chamas no seu avatar", "AVATAR_FRAME", 400, 9),
            new CatalogItem("FRAME_AMETHYST", "Moldura Ametista", "Borda roxa pulsante", "AVATAR_FRAME", 480, 11),
            new CatalogItem("FRAME_RAINBOW", "Moldura Arco-íris", "Borda com gradiente animado", "AVATAR_FRAME", 550, 13),
            new CatalogItem("FRAME_ROYAL", "Moldura Real", "Borda dourada real com brilho", "AVATAR_FRAME", 700, 15),
            // ---- Banners (faixa de fundo) ----
            new CatalogItem("BANNER_MEADOW", "Banner Campina", "Faixa de fundo verde suave", "BANNER", 120, 1),
            new CatalogItem("BANNER_OCEAN", "Banner Oceano", "Faixa de fundo azul do oceano", "BANNER", 200, 2),
            new CatalogItem("BANNER_FOREST", "Banner Floresta", "Faixa de fundo verde da floresta", "BANNER", 200, 3),
            new CatalogItem("BANNER_SUNSET", "Banner Pôr do Sol", "Faixa de fundo em tons quentes", "BANNER", 250, 4),
            new CatalogItem("BANNER_CANDY", "Banner Doce", "Faixa de fundo rosa-algodão", "BANNER", 260, 5),
            new CatalogItem("BANNER_CYBER", "Banner Cyber", "Faixa de fundo neon animada", "BANNER", 500, 10),
            new CatalogItem("BANNER_LAVA", "Banner Lava", "Faixa de fundo de lava animada", "BANNER", 550, 12),
            new CatalogItem("BANNER_GALAXY", "Banner Galáxia", "Faixa de fundo estrelada animada", "BANNER", 600, 13),
            new CatalogItem("BANNER_AURORA", "Banner Aurora", "Faixa de fundo aurora boreal animada", "BANNER", 650, 15),
            // ---- Fonte do nome ----
            new CatalogItem("FONT_MONO", "Fonte Monoespaçada", "Seu nome em fonte monoespaçada", "FONT", 120, 1),
            new CatalogItem("FONT_UNBOUNDED", "Fonte Unbounded", "Seu nome na fonte gamer Unbounded", "FONT", 180, 3),
            new CatalogItem("FONT_RETRO", "Fonte Retrô", "Seu nome em fonte retrô (VT323)", "FONT", 220, 5),
            new CatalogItem("FONT_BUNGEE", "Fonte Bungee", "Seu nome em fonte de letreiro (Bungee)", "FONT", 300, 8),
            new CatalogItem("FONT_PIXEL", "Fonte Pixel", "Seu nome em fonte pixel arcade", "FONT", 400, 11),
            // ---- Estilo do nome ----
            new CatalogItem("STYLE_ITALIC", "Nome em Itálico", "Deixa o seu nome em itálico", "NAME_STYLE", 90, 1),
            new CatalogItem("STYLE_UNDERLINE", "Nome Sublinhado", "Sublinha o seu nome", "NAME_STYLE", 110, 2),
            new CatalogItem("STYLE_STRIKE", "Nome Tachado", "Risca o seu nome", "NAME_STYLE", 110, 2),
            new CatalogItem("STYLE_SPACED", "Nome Espaçado", "Aumenta o espaçamento entre letras", "NAME_STYLE", 130, 3),
            // ---- Efeito do nome (cor/gradiente/animação) ----
            new CatalogItem("EFFECT_GOLD", "Nome Dourado", "Seu nome em dourado", "NAME_EFFECT", 200, 3),
            new CatalogItem("EFFECT_GRADIENT", "Nome Gradiente", "Seu nome com gradiente colorido", "NAME_EFFECT", 260, 5),
            new CatalogItem("EFFECT_SHINE", "Nome Brilhante", "Seu nome com brilho passando", "NAME_EFFECT", 320, 7),
            new CatalogItem("EFFECT_GLOW", "Nome Neon", "Seu nome com brilho neon", "NAME_EFFECT", 360, 8),
            new CatalogItem("EFFECT_RAINBOW", "Nome Arco-íris", "Seu nome com gradiente animado", "NAME_EFFECT", 420, 10),
            new CatalogItem("EFFECT_WAVE", "Nome Onda", "Seu nome com as letras em onda", "NAME_EFFECT", 480, 12),
            new CatalogItem("EFFECT_GLITCH", "Nome Glitch", "Seu nome com efeito glitch", "NAME_EFFECT", 550, 14)
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
                        item.price(), owned.contains(item.code()), item.requiredLevel()))
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

        if (user.getLevel() < item.requiredLevel()) {
            throw new IllegalArgumentException("Item bloqueado: requer nível " + item.requiredLevel());
        }

        boolean charged = walletService.spend(user, item.price(), "Compra: " + item.name());
        if (!charged) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        inventoryRepository.save(new UserInventoryItem(user, itemCode));
        return new StoreItemResponse(item.code(), item.name(), item.description(), item.category(), item.price(), true, item.requiredLevel());
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
            case "FONT" -> user.setEquippedFont(itemCode);
            case "NAME_STYLE" -> user.setEquippedNameStyle(itemCode);
            case "NAME_EFFECT" -> user.setEquippedNameEffect(itemCode);
            default -> throw new IllegalArgumentException("Categoria inválida: " + category);
        }
    }
}
