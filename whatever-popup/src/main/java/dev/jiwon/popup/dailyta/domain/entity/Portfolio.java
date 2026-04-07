package dev.jiwon.popup.dailyta.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portfolio")
@Getter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(name = "buy_price")
    private Double buyPrice;

    public Portfolio(String ticker, Double buyPrice) {
        this.ticker = ticker;
        this.buyPrice = buyPrice;
    }
}
