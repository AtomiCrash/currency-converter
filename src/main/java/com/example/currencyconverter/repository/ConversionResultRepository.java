package com.example.currencyconverter.repository;

import com.example.currencyconverter.entity.ConversionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversionResultRepository extends JpaRepository<ConversionResult, Long> {
}