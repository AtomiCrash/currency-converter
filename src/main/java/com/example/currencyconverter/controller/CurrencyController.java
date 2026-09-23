package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.BulkConversionRequest;
import com.example.currencyconverter.dto.BulkConversionResponse;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.service.CurrencyService;
import com.example.currencyconverter.service.RequestCounterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final RequestCounterService counterService;

    public CurrencyController(CurrencyService currencyService, RequestCounterService counterService) {
        this.currencyService = currencyService;
        this.counterService = counterService;
    }

    @GetMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(
            @RequestParam("amount") double amount,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        return ResponseEntity.ok(currencyService.convert(amount, from, to));
    }

    @PostMapping("/convert/bulk")
    public ResponseEntity<BulkConversionResponse> convertBulk(@Valid @RequestBody BulkConversionRequest request) {
        return ResponseEntity.ok(new BulkConversionResponse(
                currencyService.convertBulk(request.getAmounts(), request.getFromCurrency(), request.getToCurrency())));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of("requests", counterService.getCount()));
    }
}