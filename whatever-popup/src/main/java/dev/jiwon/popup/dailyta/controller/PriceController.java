package dev.jiwon.popup.dailyta.controller;

import dev.jiwon.popup.dailyta.dto.PriceInfo;
import dev.jiwon.popup.dailyta.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService service;

    @GetMapping("/prices")
    public Map<String, PriceInfo> getPrices() {
        return service.getPrices();
    }
}
