package dev.jiwon.popup.dailyta.dto;

public record PriceInfo(
    Double price,
    Double changePct,
    String updatedAt
) {
}
