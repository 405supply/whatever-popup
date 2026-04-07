package dev.jiwon.popup.dailyta.controller;

import dev.jiwon.popup.dailyta.dto.StockItem;
import dev.jiwon.popup.dailyta.StaticStockData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @GetMapping("/nasdaq200")
    public List<StockItem> getNasdaq200() {
        return StaticStockData.NASDAQ_200;
    }

    @GetMapping("/kospi100")
    public List<StockItem> getKospi100() {
        return StaticStockData.KOSPI_100;
    }
}
