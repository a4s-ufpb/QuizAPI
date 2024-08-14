package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.user.*;
import br.ufpb.dcx.apps4society.quizapi.service.UserService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserAlreadyExistsException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/user")
@Tag(name = "User", description = "Users of Quiz")
public class UserController {
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(tags = "User", summary = "Register User", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid UserRequest userRequest) throws UserAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(userRequest));
    }

    @Operation(tags = "User", summary = "Login User", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> loginUser(@RequestBody @Valid UserLogin userLogin){
        return ResponseEntity.ok(userService.loginUser(userLogin));
    }

    @Operation(tags = "User", summary = "Find User", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/find")
    public ResponseEntity<UserResponse> findUser(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.findUser(token));
    }

    @Operation(tags = "User", summary = "Find All Users", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/all/{userId}")
    public ResponseEntity<Page<UserResponse>> findAllUsers(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                           @RequestParam(value = "size", defaultValue = "12") Integer size,
                                                           @RequestHeader("Authorization") String token,
                                                           @PathVariable UUID userId) throws UserNotHavePermissionException {
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(userService.findAllUsers(pageable,token, userId));
    }

    @Operation(tags = "User", summary = "Remove User", responses ={
            @ApiResponse(description = "No Content", responseCode = "204", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeUser(@PathVariable UUID id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        userService.removeUser(id, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(tags = "User", summary = "Update User", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody @Valid UserUpdate userUpdate,
                                                   @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(userService.updateUser(id, userUpdate, token));
    }

    @Operation(tags = "User", summary = "Update Password", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content()),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/password/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id, @RequestBody @Valid UserUpdatePassword userUpdatePassword,
                                                   @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        userService.updatePassword(id, userUpdatePassword, token);
        return ResponseEntity.ok().build();
    }

    @Operation(tags = "User", summary = "Validade User Admin", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/admin/{id}")
    public ResponseEntity<AdminResponse> validadeUserAdmin(@RequestHeader("Authorization") String token,
                                                           @PathVariable UUID id) throws UserNotHavePermissionException {
        return ResponseEntity.ok(userService.validateIfUserIsAdmin(token, id));
    }
}
