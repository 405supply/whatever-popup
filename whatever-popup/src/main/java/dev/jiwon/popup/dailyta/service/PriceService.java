package dev.jiwon.popup.dailyta.service;

import dev.jiwon.popup.dailyta.dto.PriceInfo;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final PortfolioService portfolioService;
    private final RestClient restClient;

    private final Map<String, PriceInfo> cache = new ConcurrentHashMap<>();

    private static final String YF_QUOTE_URL =
        "https://query2.finance.yahoo.com/v7/finance/quote?symbols={symbols}";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public Map<String, PriceInfo> getPrices() {
        return cache;
    }

    @Scheduled(fixedRate = 60_000)
    public void refreshPrices() {
        List<String> tickers = portfolioService.getTickers();
        if (tickers.isEmpty()) {
            cache.clear();
            return;
        }

        String symbols = String.join(",", tickers);
        try {
            JsonNode root = restClient.get()
                .uri(YF_QUOTE_URL, symbols)
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .body(JsonNode.class);

            JsonNode results = root.path("quoteResponse").path("result");
            String now = LocalTime.now().format(TIME_FMT);

            for (JsonNode item : results) {
                String ticker = item.path("symbol").asText();
                JsonNode priceNode = item.path("regularMarketPrice");
                JsonNode changePctNode = item.path("regularMarketChangePercent");

                Double price = priceNode.isMissingNode() ? null : round2(priceNode.asDouble());
                Double changePct = changePctNode.isMissingNode() ? null : round2(changePctNode.asDouble());

                cache.put(ticker, new PriceInfo(price, changePct, now));
            }
        } catch (Exception e) {
            log.warn("[PriceService] 가격 갱신 실패: {}", e.getMessage());
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
