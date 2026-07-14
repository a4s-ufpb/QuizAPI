package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.*;

class StoreControllerTest extends QuizApplicationTests {
    static final String ITEM = "TITLE_ROOKIE"; // item mais barato do catálogo (80 moedas)

    private String storePath() {
        return baseURI + ":" + port + basePath + "/store";
    }

    private String matchHistoryPath() {
        return baseURI + ":" + port + basePath + "/match-history";
    }

    private String authenticate() {
        UserRequestUtil.post(mockUser.mockRequest(1));
        return UserRequestUtil.login(mockUser.mockUserLogin());
    }

    @Test
    void findCatalog_shouldReturn200() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(storePath() + "/items")
                .then()
                .statusCode(200);
    }

    @Test
    void purchase_withoutCoins_shouldReturn400() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(storePath() + "/purchase/" + ITEM)
                .then()
                .statusCode(400);
    }

    @Test
    void purchaseThenEquip_afterEarningCoins_shouldSucceed() {
        String token = authenticate();

        // Gabaritar uma partida credita moedas suficientes (10*30 + 50 = 350).
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("mode", "SINGLE_PLAYER", "themeName", "Geografia", "score", 10, "total", 10))
                .when()
                .post(matchHistoryPath())
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(storePath() + "/purchase/" + ITEM)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .patch(storePath() + "/equip/" + ITEM)
                .then()
                .statusCode(200);
    }

    @Test
    void findCatalog_withoutToken_shouldReturn403() {
        given()
                .when()
                .get(storePath() + "/items")
                .then()
                .statusCode(403);
    }
}
