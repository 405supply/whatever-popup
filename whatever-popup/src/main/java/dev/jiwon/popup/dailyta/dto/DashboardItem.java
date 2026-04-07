package dev.jiwon.popup.dailyta.dto;

public record DashboardItem(
    Long id,
    String ticker,
    Double buyPrice,
    Double rsi,
    Double macd,
    String signal,
    String analysisDate
) {
}
