package dev.jiwon.popup.dailyta.domain.repository;

import dev.jiwon.popup.dailyta.domain.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Analysis a WHERE a.ticker = :ticker AND a.analysisDate = :date")
    void deleteByTickerAndDate(String ticker, String date);

    @Query("SELECT a FROM Analysis a WHERE a.ticker = :ticker ORDER BY a.analysisDate DESC, a.id DESC")
    List<Analysis> findHistoryByTicker(String ticker, org.springframework.data.domain.Pageable pageable);

    @Query(value = """
        SELECT p.id, p.ticker, p.buy_price,
               a.rsi, a.macd, a.signal, a.analysis_date
        FROM portfolio p
        LEFT JOIN analysis a ON p.ticker = a.ticker
            AND a.id = (
                SELECT MAX(a2.id) FROM analysis a2 WHERE a2.ticker = p.ticker
            )
        """, nativeQuery = true)
    List<Object[]> findDashboardData();
}
