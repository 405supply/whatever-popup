package dev.jiwon.popup.dailyta.controller;

import dev.jiwon.popup.dailyta.domain.entity.Analysis;
import dev.jiwon.popup.dailyta.service.AnalysisService;
import dev.jiwon.popup.dailyta.dto.DashboardItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService service;

    @GetMapping("/analyze/{ticker}")
    public Analysis analyzeSingle(@PathVariable String ticker) {
        return service.analyzeStock(ticker.toUpperCase());
    }

    @GetMapping("/analyze-all")
    public Map<String, Object> analyzeAll() {
        return service.analyzeAll();
    }

    @GetMapping("/history/{ticker}")
    public List<Analysis> getHistory(@PathVariable String ticker) {
        return service.getHistory(ticker.toUpperCase());
    }

    @GetMapping("/dashboard")
    public List<DashboardItem> getDashboard() {
        return service.getDashboard();
    }
}
