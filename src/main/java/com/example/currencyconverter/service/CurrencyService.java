package com.example.currencyconverter.service;

import com.example.currencyconverter.cache.CurrencyCache;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.entity.ConversionResult;
import com.example.currencyconverter.exception.InvalidParameterException;
import com.example.currencyconverter.repository.ConversionResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private final RestTemplate restTemplate;
    private final CurrencyCache cache;
    private final ConversionResultRepository repository;
    private final RequestCounterService counterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${currency.api.url:https://open.er-api.com/v6/latest/}")
    private String apiUrl;

    public CurrencyService(RestTemplate restTemplate,
                           CurrencyCache cache,
                           ConversionResultRepository repository,
                           RequestCounterService counterService) {
        this.restTemplate = restTemplate;
        this.cache = cache;
        this.repository = repository;
        this.counterService = counterService;
    }

    public ConversionResponse convert(double amount, String from, String to) {
        counterService.increment();
        validate(amount, from, to);
        double rate = getRate(from.toUpperCase(), to.toUpperCase());
        double result = amount * rate;
        ConversionResult saved = new ConversionResult(amount, from.toUpperCase(), to.toUpperCase(), result, LocalDateTime.now());
        repository.save(saved);
        log.info("Converted {} {} to {} = {}", amount, from, to, result);
        return new ConversionResponse(amount, from.toUpperCase(), to.toUpperCase(), result);
    }

    public List<ConversionResponse> convertBulk(List<Double> amounts, String from, String to) {
        return amounts.stream()
                .map(a -> convert(a, from, to))
                .collect(Collectors.toList());
    }

    private void validate(double amount, String from, String to) {
        if (amount <= 0) {
            throw new InvalidParameterException("Amount must be positive");
        }
        if (from == null || from.length() != 3) {
            throw new InvalidParameterException("Invalid source currency code");
        }
        if (to == null || to.length() != 3) {
            throw new InvalidParameterException("Invalid target currency code");
        }
    }

    private double getRate(String from, String to) {
        if (from.equals(to)) {
            return 1.0;
        }
        String key = from + "_" + to;
        if (cache.contains(key)) {
            log.debug("Cache hit for {}", key);
            return cache.get(key);
        }
        try {
            String url = apiUrl + from;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            String result = root.path("result").asText();
            if (!"success".equals(result)) {
                throw new InvalidParameterException("Unsupported currency code: " + from);
            }

            JsonNode rates = root.get("rates");
            if (rates == null || !rates.has(to)) {
                throw new InvalidParameterException("Currency not found: " + to);
            }
            double rate = rates.get(to).asDouble();
            cache.put(key, rate);
            log.debug("Fetched rate {} = {}", key, rate);
            return rate;
        } catch (InvalidParameterException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch rate", ex);
            throw new RuntimeException("Failed to fetch currency rate", ex);
        }
    }
}