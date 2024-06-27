package br.ufpb.dcx.apps4society.quizapi.util;

import br.ufpb.dcx.apps4society.quizapi.dto.user.UserLogin;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.mock.MockUser;
import br.ufpb.dcx.apps4society.quizapi.dto.user.TokenResponse;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static io.restassured.RestAssured.*;

public class UserRequestUtil {

    public static final String BASE_PATH_REGISTER = "/user/register";
    public static final String BASE_PATH_LOGIN = "/user/login";
    public static final String BASE_PATH_USER = "/user/";

    @BeforeEach
    void setUp(){
        port = 8080;
        baseURI = "http://localhost";
        basePath = "/v1";
    }

    public static UserResponse post(UserRequest userRequest){
        return given()
                .contentType(ContentType.JSON)
                .body(userRequest)
                .when()
                .post(baseURI+":"+port+basePath+BASE_PATH_REGISTER)
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(UserResponse.class);
    }

    public static String login(UserLogin userLogin){
        return given()
                .contentType(ContentType.JSON)
                .body(userLogin)
                .when()
                .post(baseURI+":"+port+basePath+BASE_PATH_LOGIN)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(TokenResponse.class)
                .token();
    }

    public static void delete(UUID uuid, String token){
        given()
                .header("Authorization", "Bearer "+token)
                .delete(baseURI+":"+port+basePath+BASE_PATH_USER+uuid)
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    public static void delete(UserResponse userResponse){
        String token = login(new MockUser().mockUserLogin());
        delete(userResponse.uuid(), token);
    }
}
