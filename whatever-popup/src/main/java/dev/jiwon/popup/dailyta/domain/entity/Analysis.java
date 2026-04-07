package dev.jiwon.popup.dailyta.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "analysis_date", nullable = false)
    private String analysisDate;

    private Double rsi;
    private Double macd;

    @Column(columnDefinition = "TEXT")
    private String signal;

    @Builder
    public Analysis(String ticker, String analysisDate, Double rsi, Double macd, String signal) {
        this.ticker = ticker;
        this.analysisDate = analysisDate;
        this.rsi = rsi;
        this.macd = macd;
        this.signal = signal;
    }
}
