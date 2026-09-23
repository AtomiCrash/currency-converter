package com.example.currencyconverter.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CurrencyCache {
    private final Map<String, Double> cache = new ConcurrentHashMap<>();

    public Double get(String key) {
        return cache.get(key);
    }

    public void put(String key, Double value) {
        cache.put(key, value);
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public void clear() {
        cache.clear();
    }
}