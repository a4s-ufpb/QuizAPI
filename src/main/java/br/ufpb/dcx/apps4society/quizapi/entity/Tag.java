package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity(name = "tb_tag")
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_creator_name", columnNames = {"creator_uuid", "name"})
}, indexes = {
        @Index(name = "idx_tag_creator", columnList = "creator_uuid")
})
public class Tag implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_uuid")
    private User creator;

    public Tag() {}

    public Tag(String name, User creator) {
        this.name = name;
        this.creator = creator;
    }

    public TagResponse entityToResponse() {
        return new TagResponse(id, name, creator.getUuid());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public User getCreator() { return creator; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag tag)) return false;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
