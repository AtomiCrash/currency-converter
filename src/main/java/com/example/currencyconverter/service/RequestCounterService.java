package com.example.currencyconverter.service;

import org.springframework.stereotype.Service;

@Service
public class RequestCounterService {
    private long counter = 0;

    public synchronized void increment() {
        counter++;
    }

    public synchronized long getCount() {
        return counter;
    }
}