package dev.jiwon.popup.dailyta.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ticker")
    private String ticker;

    @Builder
    public Stock(
            Long id,
            String name,
            String ticker
    ) {
        this.id = id;
        this.name = name;
        this.ticker = ticker;
    }
}
