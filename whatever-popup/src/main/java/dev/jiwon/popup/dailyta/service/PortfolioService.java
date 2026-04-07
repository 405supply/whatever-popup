package dev.jiwon.popup.dailyta.service;

import dev.jiwon.popup.dailyta.domain.entity.Portfolio;
import dev.jiwon.popup.dailyta.domain.repository.PortfolioRepository;
import dev.jiwon.popup.dailyta.dto.PortfolioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository repository;

    public List<Portfolio> getAll() {
        return repository.findAll();
    }

    public Map<String, String> add(PortfolioRequest request) {
        String ticker = request.ticker().toUpperCase();
        if (repository.existsByTicker(ticker)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 존재하는 티커입니다.");
        }
        repository.save(new Portfolio(ticker, request.buyPrice()));
        return Map.of("message", ticker + " 추가 완료");
    }

    public Map<String, String> delete(String ticker) {
        Portfolio portfolio = repository.findByTicker(ticker.toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "티커를 찾을 수 없습니다."));
        repository.delete(portfolio);
        return Map.of("message", ticker.toUpperCase() + " 삭제 완료");
    }

    public List<String> getTickers() {
        return repository.findAllTickers();
    }
}
