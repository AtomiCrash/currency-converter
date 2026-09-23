package com.example.currencyconverter;

import com.example.currencyconverter.controller.CurrencyController;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.exception.GlobalExceptionHandler;
import com.example.currencyconverter.exception.InvalidParameterException;
import com.example.currencyconverter.service.CurrencyService;
import com.example.currencyconverter.service.RequestCounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
@Import(GlobalExceptionHandler.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private RequestCounterService counterService;

    @Test
    void testConvertEndpoint() throws Exception {
        when(currencyService.convert(anyDouble(), anyString(), anyString()))
                .thenReturn(new ConversionResponse(100.0, "USD", "EUR", 92.0));

        mockMvc.perform(get("/api/currency/convert")
                        .param("amount", "100")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("EUR"))
                .andExpect(jsonPath("$.result").value(92.0));
    }

    @Test
    void testConvertInvalidParameterReturns400() throws Exception {
        when(currencyService.convert(anyDouble(), anyString(), anyString()))
                .thenThrow(new InvalidParameterException("Amount must be positive"));

        mockMvc.perform(get("/api/currency/convert")
                        .param("amount", "-1")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount must be positive"));
    }

    @Test
    void testConvertInternalErrorReturns500() throws Exception {
        when(currencyService.convert(anyDouble(), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/currency/convert")
                        .param("amount", "100")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void testBulkEndpoint() throws Exception {
        when(currencyService.convertBulk(anyList(), anyString(), anyString()))
                .thenReturn(List.of(
                        new ConversionResponse(10.0, "USD", "EUR", 9.0),
                        new ConversionResponse(20.0, "USD", "EUR", 18.0)));

        String body = "{\"amounts\":[10,20],\"fromCurrency\":\"USD\",\"toCurrency\":\"EUR\"}";

        mockMvc.perform(post("/api/currency/convert/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].result").value(9.0))
                .andExpect(jsonPath("$.results[1].result").value(18.0));
    }

    @Test
    void testStatsEndpoint() throws Exception {
        when(counterService.getCount()).thenReturn(42L);

        mockMvc.perform(get("/api/currency/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").value(42));
    }
}