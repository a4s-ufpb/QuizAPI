package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.room.Player;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity(name = "tb_user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;
    private String name;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    private int likes = 0;
    private int xp = 0;
    private int level = 1;
    private int coins = 0;
    // Cosméticos equipados (códigos de itens da loja, null = nenhum). Puramente
    // estético: título ao lado do nome, moldura no avatar e banner de fundo.
    @Column(name = "equipped_title")
    private String equippedTitle;
    @Column(name = "equipped_frame")
    private String equippedFrame;
    @Column(name = "equipped_banner")
    private String equippedBanner;
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Theme> themes = new ArrayList<>();
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Response> responses = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Score> scores = new ArrayList<>();
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Room> rooms = new ArrayList<>();

    public User(){

    }

    public User(UserRequest userRequest) {
        this.name = userRequest.name();
        this.email = userRequest.email();
        this.password = userRequest.password();
        this.role = Role.USER;
    }

    public User(UUID uuid, String name, String email, String password, Role role) {
        this.uuid = uuid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UserResponse entityToResponse(){
        return new UserResponse(uuid,name,email,role,likes,xp,level,coins,
                equippedTitle,equippedFrame,equippedBanner);
    }

    public String getEquippedTitle() { return equippedTitle; }
    public String getEquippedFrame() { return equippedFrame; }
    public String getEquippedBanner() { return equippedBanner; }
    public void setEquippedTitle(String equippedTitle) { this.equippedTitle = equippedTitle; }
    public void setEquippedFrame(String equippedFrame) { this.equippedFrame = equippedFrame; }
    public void setEquippedBanner(String equippedBanner) { this.equippedBanner = equippedBanner; }

    public void addLike(){
        this.likes++;
    }

    public int getLikes() {
        return likes;
    }

    // Cada 100 xp sobe um nível (nível mínimo 1).
    private static final int XP_PER_LEVEL = 100;

    public void addXp(int amount){
        this.xp += amount;
        this.level = 1 + (this.xp / XP_PER_LEVEL);
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    /** @return true se tinha saldo suficiente e o débito foi aplicado. */
    public boolean spendCoins(int amount) {
        if (coins < amount) return false;
        coins -= amount;
        return true;
    }

    public void addTheme(Theme theme){
        this.themes.add(theme);
    }

    public void addQuestion(Question question){
        this.questions.add(question);
    }

    public void addResponse(Response response){
        this.responses.add(response);
    }

    public boolean userNotHavePermission(User user){
        return !this.equals(user) && this.getRole() == Role.USER;
    }

    public Player convertUserToPlayer() {
        return new Player(uuid, name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(uuid, user.uuid) && Objects.equals(name, user.name) && Objects.equals(email, user.email) && Objects.equals(password, user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, email, password);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == Role.ADMIN) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        else return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Role getRole() {
        return role;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }
}
