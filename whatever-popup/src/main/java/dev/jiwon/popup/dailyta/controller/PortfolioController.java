package dev.jiwon.popup.dailyta.controller;

import dev.jiwon.popup.dailyta.domain.entity.Portfolio;
import dev.jiwon.popup.dailyta.dto.PortfolioRequest;
import dev.jiwon.popup.dailyta.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService service;

    @GetMapping
    public List<Portfolio> getPortfolio() {
        return service.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addPortfolio(@RequestBody PortfolioRequest request) {
        return service.add(request);
    }

    @DeleteMapping("/{ticker}")
    public Map<String, String> deletePortfolio(@PathVariable String ticker) {
        return service.delete(ticker);
    }
}
