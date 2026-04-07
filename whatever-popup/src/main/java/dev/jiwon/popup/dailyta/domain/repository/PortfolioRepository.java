package dev.jiwon.popup.dailyta.domain.repository;

import dev.jiwon.popup.dailyta.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    @Query("SELECT p.ticker FROM Portfolio p")
    List<String> findAllTickers();
}
