package com.example.currencyconverter.dto;

import java.util.List;

public class BulkConversionResponse {
    private List<ConversionResponse> results;

    public BulkConversionResponse() {
    }

    public BulkConversionResponse(List<ConversionResponse> results) {
        this.results = results;
    }

    public List<ConversionResponse> getResults() {
        return results;
    }

    public void setResults(List<ConversionResponse> results) {
        this.results = results;
    }
}