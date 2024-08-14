package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.user.*;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.security.TokenProvider;
import br.ufpb.dcx.apps4society.quizapi.service.exception.*;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    private TokenProvider tokenProvider;

    @Autowired
    public UserService(UserRepository userRepository, AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public UserResponse registerUser(UserRequest userRequest) throws UserAlreadyExistsException {
        User user = (User) userRepository.findByEmail(userRequest.email());

        if (user != null){
            throw new UserAlreadyExistsException("Tente se registrar com outro email");
        }

        User saveUser = new User(userRequest);
        saveUser.setPassword(passwordEncoder.encode(userRequest.password()));

        userRepository.save(saveUser);

        return saveUser.entityToResponse();
    }

    public TokenResponse loginUser(UserLogin userLogin){
        UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(userLogin.email(), userLogin.password());
        Authentication auth = authenticationManager.authenticate(user);
        String token = tokenProvider.generateToken((User) auth.getPrincipal());
        return new TokenResponse(token);
    }

    public UserResponse findUser(String token) {
        return findUserByToken(token).entityToResponse();
    }

    public void removeUser(UUID id, String token) throws UserNotHavePermissionException {
        User loggedUser = findUserByToken(token);

        User removeUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND));

        if (loggedUser.userNotHavePermission(removeUser)){
            throw new UserNotHavePermissionException("Você não tem permissão para remover esse usuário");
        }

        userRepository.delete(removeUser);
    }

    public Page<UserResponse> findAllUsers(Pageable pageable, String token, UUID userId) throws UserNotHavePermissionException {
        Page<User> users = userRepository.findAll(pageable);

        boolean isAdmin = validateIfUserIsAdmin(token, userId).isAdmin();

        if (!isAdmin){
            throw new UserNotHavePermissionException("Você não tem permissão para realizar essa funcionalidade");
        }

        return users.map(User::entityToResponse);
    }

    public UserResponse updateUser(UUID id, UserUpdate userUpdate, String token) throws UserNotHavePermissionException {
        User loggedUser = findUserByToken(token);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND));

        if (loggedUser.userNotHavePermission(user)){
            throw new UserNotHavePermissionException("Você não tem permissão para atualizar esse usuário");
        }

        updateData(user, userUpdate);
        userRepository.save(user);

        return user.entityToResponse();
    }

    private void updateData(User user, UserUpdate userUpdate) {
        user.setName(userUpdate.name());
    }

    public void updatePassword(UUID id, UserUpdatePassword userUpdatePassword, String token) throws UserNotHavePermissionException {
        User loggedUser = findUserByToken(token);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND));

        if (loggedUser.userNotHavePermission(user)){
            throw new UserNotHavePermissionException("Você não tem permissão para realizar essa funcionalidade");
        }

        if (!isValidPassword(userUpdatePassword)){
            throw new IllegalArgumentException("Senhas inválidas");
        }

        user.setPassword(passwordEncoder.encode(userUpdatePassword.newPassword()));
        userRepository.save(user);
    }

    private boolean isValidPassword(UserUpdatePassword userUpdatePassword) {
        return userUpdatePassword.newPassword().equals(userUpdatePassword.confirmNewPassword());
    }

    public User findUserByToken(String token) {
        if (token != null && token.startsWith("Bearer ")){
            token = token.substring("Bearer ".length());
        }

        String email = tokenProvider.getSubjectByToken(token);

        User user = (User) userRepository.findByEmail(email);

        if (user == null){
            throw new InvalidUserException("Usuário inválido, pode ter sido removido do BD e utilizado o token");
        }

        return user;
    }

    public AdminResponse validateIfUserIsAdmin(String token, UUID id) throws UserNotHavePermissionException {
        User loggedUser = findUserByToken(token);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND));

        if (loggedUser.userNotHavePermission(user)){
            throw new UserNotHavePermissionException("Você não tem permissão para realizar essa funcionalidade");
        }

        return new AdminResponse(
                loggedUser
                .getRole()
                .equals(Role.ADMIN));
    }
}
