package dev.jiwon.popup.dailyta.service;

import dev.jiwon.popup.dailyta.dto.DashboardItem;
import dev.jiwon.popup.dailyta.domain.entity.Analysis;
import dev.jiwon.popup.dailyta.domain.repository.AnalysisRepository;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository repository;
    private final PortfolioService portfolioService;
    private final RestClient restClient;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String YF_CHART_URL = "https://query2.finance.yahoo.com/v8/finance/chart/{ticker}?range=6mo&interval=1d";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={key}";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter KR_DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    // ── Public API ──────────────────────────────────────────────

    public Analysis analyzeStock(String ticker) {
        double[] closes = fetchCloses(ticker.toUpperCase());
        double[] volumes = fetchVolumes(ticker.toUpperCase());

        if (closes.length < 130) {
            throw new IllegalStateException(ticker + ": 데이터 부족 (최소 130일 필요)");
        }

        int n = closes.length;
        double rsiVal   = calcRSI(closes, 14);
        double[] macdLine   = calcMACDLine(closes);
        double[] signalLine = calcSignalLine(macdLine);
        double macdVal    = macdLine[n - 1];
        double signalVal  = signalLine[n - 1];
        double histVal    = macdVal - signalVal;
        double histPrev   = macdLine[n - 2] - signalLine[n - 2];

        double sma20  = calcSMA(closes, n - 1, 20);
        double sma50  = calcSMA(closes, n - 1, 50);
        double sma120 = calcSMA(closes, n - 1, 120);
        double bbMid  = sma20;
        double bbStd  = calcStdDev(closes, n - 1, 20);
        double bbUpper = bbMid + 2 * bbStd;
        double bbLower = bbMid - 2 * bbStd;

        String today    = LocalDate.now().format(DATE_FMT);
        String todayKr  = LocalDate.now().format(KR_DATE_FMT);
        String alignment = maAlignment(sma20, sma50, sma120);
        String histState = histState(histVal, histPrev);

        String prompt = buildPrompt(
            ticker, todayKr,
            closes[n - 1], volumes[n - 1], volumes[n - 2],
            sma20, sma50, sma120, alignment,
            rsiVal,
            macdVal, signalVal, histVal, histState,
            bbUpper, bbMid, bbLower
        );

        log.info("[Gemini] {} 요청 시작", ticker);
        String signal = callGemini(prompt);
        log.info("[Gemini] {} 응답 완료: {}...", ticker, signal.substring(0, Math.min(80, signal.length())));

        repository.deleteByTickerAndDate(ticker.toUpperCase(), today);
        Analysis saved = repository.save(
            new Analysis(ticker.toUpperCase(), today, round2(rsiVal), round4(macdVal), signal)
        );
        return saved;
    }

    public Map<String, Object> analyzeAll() {
        List<String> tickers = portfolioService.getTickers();
        List<Analysis> results = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();
        for (String ticker : tickers) {
            try {
                results.add(analyzeStock(ticker));
            } catch (Exception e) {
                log.error("[analyzeAll] {} 실패: {}", ticker, e.getMessage());
                errors.add(Map.of("ticker", ticker, "error", e.getMessage()));
            }
        }
        return Map.of("results", results, "errors", errors);
    }

    public List<Analysis> getHistory(String ticker) {
        return repository.findHistoryByTicker(ticker.toUpperCase(), PageRequest.of(0, 30));
    }

    public List<DashboardItem> getDashboard() {
        return repository.findDashboardData().stream()
            .map(r -> new DashboardItem(
                toLong(r[0]),
                (String) r[1],
                toDouble(r[2]),
                toDouble(r[3]),
                toDouble(r[4]),
                (String) r[5],
                (String) r[6]
            ))
            .toList();
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void scheduledAnalysis() {
        log.info("[Scheduler] 자동 분석 시작");
        portfolioService.getTickers().forEach(ticker -> {
            try {
                analyzeStock(ticker);
            } catch (Exception e) {
                log.error("[Scheduler] {} 분석 실패: {}", ticker, e.getMessage());
            }
        });
    }

    // ── Yahoo Finance ──────────────────────────────────────────

    private JsonNode fetchChart(String ticker) {
        return restClient.get()
            .uri(YF_CHART_URL, ticker)
            .header("User-Agent", "Mozilla/5.0")
            .retrieve()
            .body(JsonNode.class);
    }

    private double[] fetchCloses(String ticker) {
        JsonNode root = fetchChart(ticker);
        JsonNode quote = root.path("chart").path("result").get(0)
            .path("indicators").path("quote").get(0);
        return toDoubleArray(quote.path("close"));
    }

    private double[] fetchVolumes(String ticker) {
        JsonNode root = fetchChart(ticker);
        JsonNode quote = root.path("chart").path("result").get(0)
            .path("indicators").path("quote").get(0);
        return toDoubleArray(quote.path("volume"));
    }

    private double[] toDoubleArray(JsonNode node) {
        List<Double> list = new ArrayList<>();
        for (JsonNode n : node) {
            if (!n.isNull()) list.add(n.asDouble());
        }
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // ── Technical Indicators ───────────────────────────────────

    private double calcRSI(double[] closes, int period) {
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double d = closes[i] - closes[i - 1];
            if (d > 0) avgGain += d; else avgLoss -= d;
        }
        avgGain /= period;
        avgLoss /= period;
        for (int i = period + 1; i < closes.length; i++) {
            double d = closes[i] - closes[i - 1];
            avgGain = (avgGain * (period - 1) + Math.max(d, 0)) / period;
            avgLoss = (avgLoss * (period - 1) + Math.max(-d, 0)) / period;
        }
        if (avgLoss == 0) return 100;
        return 100 - (100 / (1 + avgGain / avgLoss));
    }

    private double[] calcEMA(double[] data, int period, int startIdx) {
        double[] ema = new double[data.length];
        double mult = 2.0 / (period + 1);
        double sum = 0;
        for (int i = startIdx; i < startIdx + period; i++) sum += data[i];
        ema[startIdx + period - 1] = sum / period;
        for (int i = startIdx + period; i < data.length; i++) {
            ema[i] = data[i] * mult + ema[i - 1] * (1 - mult);
        }
        return ema;
    }

    private double[] calcMACDLine(double[] closes) {
        double[] ema12 = calcEMA(closes, 12, 0);
        double[] ema26 = calcEMA(closes, 26, 0);
        double[] macd = new double[closes.length];
        for (int i = 25; i < closes.length; i++) {
            macd[i] = ema12[i] - ema26[i];
        }
        return macd;
    }

    private double[] calcSignalLine(double[] macdLine) {
        // MACD 유효 시작: 25, signal = EMA9 of MACD (시작: 25+8=33)
        return calcEMA(macdLine, 9, 25);
    }

    private double calcSMA(double[] data, int endIdx, int period) {
        double sum = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) sum += data[i];
        return sum / period;
    }

    private double calcStdDev(double[] data, int endIdx, int period) {
        double mean = calcSMA(data, endIdx, period);
        double sumSq = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) {
            double d = data[i] - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / period);
    }

    private String maAlignment(double sma20, double sma50, double sma120) {
        if (sma20 > sma50 && sma50 > sma120) return "정배열";
        if (sma20 < sma50 && sma50 < sma120) return "역배열";
        return "혼조";
    }

    private String histState(double hist, double histPrev) {
        if (hist > 0 && histPrev <= 0) return "양전환";
        if (hist < 0 && histPrev >= 0) return "음전환";
        return (hist > 0 ? "양" : "음") + "수 유지";
    }

    // ── Gemini API ─────────────────────────────────────────────

    private String buildPrompt(
        String ticker, String dateKr,
        double price, double volume, double volumePrev,
        double sma20, double sma50, double sma120, String alignment,
        double rsi,
        double macd, double signal, double hist, String histState,
        double bbUpper, double bbMid, double bbLower
    ) {
        String template = loadPromptTemplate();
        String dataSection = String.format("""
            당신은 월스트리트에서 20년 이상의 경력을 가진 수석 기술적 분석가(Chief Technical Analyst)이자 리스크 관리에 철저한 퀀트 트레이더입니다.

            제가 아래에 제공하는 특정 종목의 기술적 지표(Technical Indicators) 데이터를 바탕으로,
            현재 시장 국면을 심층적으로 진단하고 단기 및 중장기 관점에서의 매우 구체적이고 실행 가능한 트레이딩 전략을 수립해 주십시오.

            분석 기준 일자: %s

            종목명/티커: %s

            타임프레임 (Timeframe): 1일봉(Daily) 기준

            현재가 및 거래량 (Price & Volume): 현재가 %.2f, 금일 거래량 %,.0f (전일 대비 %s)

            이동평균선 (Moving Averages): 20일선 %.2f / 50일선 %.2f / 120일선 %.2f (%s)

            RSI (14일 기준): %.2f

            MACD: MACD 라인 %.4f / 시그널 라인 %.4f / 히스토그램 %.4f (%s)

            볼린저 밴드 (20일, 2σ): 상단 %.2f / 중단 %.2f / 하단 %.2f

            """,
            dateKr, ticker,
            price, volume, volume > volumePrev ? "증가" : "감소",
            sma20, sma50, sma120, alignment,
            rsi,
            macd, signal, hist, histState,
            bbUpper, bbMid, bbLower
        );
        return dataSection + template;
    }

    private String loadPromptTemplate() {
        try {
            return new ClassPathResource("prompt.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("prompt.txt 로드 실패", e);
        }
    }

    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            ))
        );

        JsonNode response = restClient.post()
            .uri(GEMINI_URL, geminiApiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        String text = response
            .path("candidates").get(0)
            .path("content").path("parts").get(0)
            .path("text").asText("");

        if (text.isBlank()) throw new RuntimeException("Gemini 응답이 비어 있습니다.");
        return text.strip();
    }

    // ── Helpers ────────────────────────────────────────────────

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    private Long toLong(Object o) {
        if (o == null) return null;
        return ((Number) o).longValue();
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        return ((Number) o).doubleValue();
    }
}
