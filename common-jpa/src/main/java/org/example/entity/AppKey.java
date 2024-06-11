package org.example.entity;

import lombok.*;
import javax.persistence.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_key")
@Entity
public class AppKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private byte[] key;

    public AppKey setId(final Long stopId) {
        this.id = stopId;
        return this;
    }

    public AppKey setKey(final byte[] Key) {
        this.key = Key;
        return this;
    }

}
