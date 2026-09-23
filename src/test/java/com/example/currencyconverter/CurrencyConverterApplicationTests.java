package com.example.currencyconverter;

import com.example.currencyconverter.cache.CurrencyCache;
import com.example.currencyconverter.dto.BulkConversionRequest;
import com.example.currencyconverter.dto.BulkConversionResponse;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.entity.ConversionResult;
import com.example.currencyconverter.exception.InvalidParameterException;
import com.example.currencyconverter.repository.ConversionResultRepository;
import com.example.currencyconverter.service.CurrencyService;
import com.example.currencyconverter.service.RequestCounterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyServiceTest {

    private CurrencyService currencyService;
    private RestTemplate restTemplate;
    private CurrencyCache cache;
    private ConversionResultRepository repository;
    private RequestCounterService counter;

    @BeforeEach
    void setup() {
        restTemplate = Mockito.mock(RestTemplate.class);
        cache = new CurrencyCache();
        repository = Mockito.mock(ConversionResultRepository.class);
        counter = new RequestCounterService();

        currencyService = new CurrencyService(restTemplate, cache, repository, counter);
        ReflectionTestUtils.setField(currencyService, "apiUrl", "https://open.er-api.com/v6/latest/");
    }

    @Test
    void testConvertSameCurrency() {
        ConversionResponse response = currencyService.convert(100, "USD", "USD");
        assertEquals(100.0, response.getResult());
        Mockito.verifyNoInteractions(restTemplate);
    }

    @Test
    void testInvalidAmountNegative() {
        assertThrows(InvalidParameterException.class,
                () -> currencyService.convert(-5, "USD", "EUR"));
    }

    @Test
    void testInvalidAmountZero() {
        assertThrows(InvalidParameterException.class,
                () -> currencyService.convert(0, "USD", "EUR"));
    }

    @Test
    void testInvalidFromCurrencyCode() {
        assertThrows(InvalidParameterException.class,
                () -> currencyService.convert(10, "US", "EUR"));
    }

    @Test
    void testInvalidToCurrencyCode() {
        assertThrows(InvalidParameterException.class,
                () -> currencyService.convert(10, "USD", "EURO"));
    }

    @Test
    void testConvertWithMockedApi() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        ConversionResponse response = currencyService.convert(100, "USD", "EUR");

        assertEquals(90.0, response.getResult(), 0.001);
        assertEquals("USD", response.getFromCurrency());
        assertEquals("EUR", response.getToCurrency());
    }

    @Test
    void testUnknownCurrencyThrows() {
        String json = "{\"result\":\"success\",\"rates\":{\"GBP\":0.8}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        assertThrows(InvalidParameterException.class,
                () -> currencyService.convert(100, "USD", "EUR"));
    }

    @Test
    void testApiFailureThrowsRuntimeException() {
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class)))
                .thenThrow(new RuntimeException("network down"));

        assertThrows(RuntimeException.class,
                () -> currencyService.convert(100, "USD", "EUR"));
    }

    @Test
    void testCacheHitAvoidsSecondApiCall() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        currencyService.convert(100, "USD", "EUR");
        currencyService.convert(200, "USD", "EUR");

        verify(restTemplate, times(1)).getForObject(anyString(), Mockito.eq(String.class));
    }

    @Test
    void testCacheIsPopulated() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        currencyService.convert(100, "USD", "EUR");

        assertEquals(0.9, cache.get("USD_EUR"), 0.001);
    }

    @Test
    void testResultSavedToRepository() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        currencyService.convert(100, "USD", "EUR");

        verify(repository, times(1)).save(any(ConversionResult.class));
    }

    @Test
    void testCounterIncrements() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        currencyService.convert(100, "USD", "EUR");
        currencyService.convert(200, "USD", "EUR");

        assertEquals(2, counter.getCount());
    }

    @Test
    void testConvertBulkReturnsCorrectSize() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        List<ConversionResponse> results =
                currencyService.convertBulk(List.of(10.0, 20.0, 30.0), "USD", "EUR");

        assertEquals(3, results.size());
        assertEquals(9.0, results.get(0).getResult(), 0.001);
        assertEquals(18.0, results.get(1).getResult(), 0.001);
        assertEquals(27.0, results.get(2).getResult(), 0.001);
    }

    @Test
    void testConvertBulkIncrementsCounterPerItem() {
        String json = "{\"result\":\"success\",\"rates\":{\"EUR\":0.9}}";
        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class))).thenReturn(json);

        currencyService.convertBulk(List.of(10.0, 20.0, 30.0), "USD", "EUR");

        assertEquals(3, counter.getCount());
    }

    @Test
    void testCacheGetAndPut() {
        assertNull(cache.get("USD_EUR"));
        cache.put("USD_EUR", 0.9);
        assertEquals(0.9, cache.get("USD_EUR"), 0.001);
    }

    @Test
    void testCacheContains() {
        assertFalse(cache.contains("USD_EUR"));
        cache.put("USD_EUR", 0.9);
        assertTrue(cache.contains("USD_EUR"));
    }

    @Test
    void testCacheClear() {
        cache.put("USD_EUR", 0.9);
        cache.put("USD_GBP", 0.8);
        assertTrue(cache.contains("USD_EUR"));
        cache.clear();
        assertFalse(cache.contains("USD_EUR"));
        assertFalse(cache.contains("USD_GBP"));
    }

    @Test
    void testCounterInitiallyZero() {
        assertEquals(0, counter.getCount());
    }

    @Test
    void testCounterThreadSafe() throws InterruptedException {
        int threads = 10;
        int iterations = 100;
        Thread[] workers = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.increment();
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }

        assertEquals(threads * iterations, counter.getCount());
    }

    @Test
    void testConversionResultGettersSetters() {
        ConversionResult r = new ConversionResult();
        r.setId(1L);
        r.setAmount(100.0);
        r.setFromCurrency("USD");
        r.setToCurrency("EUR");
        r.setResult(90.0);
        r.setTimestamp(LocalDateTime.now());

        assertEquals(1L, r.getId());
        assertEquals(100.0, r.getAmount());
        assertEquals("USD", r.getFromCurrency());
        assertEquals("EUR", r.getToCurrency());
        assertEquals(90.0, r.getResult());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void testConversionResultConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ConversionResult r = new ConversionResult(50.0, "EUR", "USD", 55.0, now);

        assertEquals(50.0, r.getAmount());
        assertEquals("EUR", r.getFromCurrency());
        assertEquals("USD", r.getToCurrency());
        assertEquals(55.0, r.getResult());
        assertEquals(now, r.getTimestamp());
    }

    @Test
    void testBulkConversionRequestSetters() {
        BulkConversionRequest req = new BulkConversionRequest();
        req.setAmounts(List.of(1.0, 2.0, 3.0));
        req.setFromCurrency("USD");
        req.setToCurrency("EUR");

        assertEquals(3, req.getAmounts().size());
        assertEquals("USD", req.getFromCurrency());
        assertEquals("EUR", req.getToCurrency());
    }

    @Test
    void testBulkConversionResponseSetters() {
        BulkConversionResponse resp = new BulkConversionResponse();
        resp.setResults(List.of(new ConversionResponse(1.0, "USD", "EUR", 0.9)));

        assertEquals(1, resp.getResults().size());
        assertEquals(0.9, resp.getResults().get(0).getResult(), 0.001);
    }

    @Test
    void testBulkConversionResponseConstructor() {
        BulkConversionResponse resp = new BulkConversionResponse(
                List.of(new ConversionResponse(1.0, "USD", "EUR", 0.9),
                        new ConversionResponse(2.0, "USD", "EUR", 1.8)));

        assertEquals(2, resp.getResults().size());
    }

    @Test
    void testConversionResponseGettersSetters() {
        ConversionResponse resp = new ConversionResponse();
        resp.setAmount(10.0);
        resp.setFromCurrency("USD");
        resp.setToCurrency("EUR");
        resp.setResult(9.0);

        assertEquals(10.0, resp.getAmount());
        assertEquals("USD", resp.getFromCurrency());
        assertEquals("EUR", resp.getToCurrency());
        assertEquals(9.0, resp.getResult());
    }

    @Test
    void testInvalidParameterExceptionMessage() {
        InvalidParameterException ex = new InvalidParameterException("test message");
        assertEquals("test message", ex.getMessage());
    }
}