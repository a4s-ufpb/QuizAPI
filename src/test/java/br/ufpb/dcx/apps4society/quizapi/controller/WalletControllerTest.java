package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

class WalletControllerTest extends QuizApplicationTests {

    private String path() {
        return baseURI + ":" + port + basePath + "/wallet";
    }

    private String authenticate() {
        UserRequestUtil.post(mockUser.mockRequest(1));
        return UserRequestUtil.login(mockUser.mockUserLogin());
    }

    @Test
    void findMyWallet_newUser_shouldReturnZeroCoins() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path() + "/me")
                .then()
                .statusCode(200)
                .body("coins", equalTo(0));
    }

    @Test
    void findMyTransactions_shouldReturn200() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path() + "/transactions")
                .then()
                .statusCode(200);
    }

    @Test
    void findMyWallet_withoutToken_shouldReturn403() {
        given()
                .when()
                .get(path() + "/me")
                .then()
                .statusCode(403);
    }
}
