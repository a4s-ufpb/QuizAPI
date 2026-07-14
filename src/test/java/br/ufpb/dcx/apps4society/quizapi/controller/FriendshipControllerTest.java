package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserLogin;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;

class FriendshipControllerTest extends QuizApplicationTests {

    private String path() {
        return baseURI + ":" + port + basePath + "/friendship";
    }

    private record Party(UserResponse user, String token) {}

    private Party register(String name, String email) {
        UserResponse user = UserRequestUtil.post(new UserRequest(name, email, "12345678"));
        String token = UserRequestUtil.login(new UserLogin(email, "12345678"));
        return new Party(user, token);
    }

    @Test
    void requestAcceptAndListFriendship_fullFlow() {
        Party requester = register("Requester", "requester@gmail.com");
        Party addressee = register("Addressee", "addressee@gmail.com");

        // Solicita amizade
        int friendshipId = given()
                .header("Authorization", "Bearer " + requester.token())
                .when()
                .post(path() + "/request/" + addressee.user().uuid())
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Destinatário vê a solicitação pendente
        given()
                .header("Authorization", "Bearer " + addressee.token())
                .when()
                .get(path() + "/pending")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1));

        // Destinatário aceita
        given()
                .header("Authorization", "Bearer " + addressee.token())
                .when()
                .patch(path() + "/" + friendshipId + "/accept")
                .then()
                .statusCode(200);

        // Ambos passam a se ver como amigos
        given()
                .header("Authorization", "Bearer " + requester.token())
                .when()
                .get(path() + "/mine")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    void requestFriendship_withoutToken_shouldReturn403() {
        given()
                .when()
                .post(path() + "/request/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(403);
    }
}
