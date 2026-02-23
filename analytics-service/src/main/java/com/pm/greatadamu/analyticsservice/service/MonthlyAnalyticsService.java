package com.pm.greatadamu.analyticsservice.service;

import com.pm.greatadamu.analyticsservice.dto.AnalyticsRequestDTO;
import com.pm.greatadamu.analyticsservice.dto.AnalyticsResponseDTO;
import com.pm.greatadamu.analyticsservice.kafka.TransactionEvent;
import com.pm.greatadamu.analyticsservice.mapper.MonthlyAnalyticsMapper;
import com.pm.greatadamu.analyticsservice.model.Month;
import com.pm.greatadamu.analyticsservice.model.MonthlyAnalytics;
import com.pm.greatadamu.analyticsservice.repository.MonthlyAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyAnalyticsService {
    private final MonthlyAnalyticsRepository monthlyAnalyticsRepository;
    private final MonthlyAnalyticsMapper monthlyAnalyticsMapper;

  public AnalyticsResponseDTO getMonthlyAnalytics(Long customerId, Month month, int year) {
      //get the month and year analytics from db
     MonthlyAnalytics analytics = monthlyAnalyticsRepository
             .findByCustomerIdAndMonthAndYear(customerId,month,year)
             .orElse(null);

      //analytics entity -> analyticsResponse user can see
      return monthlyAnalyticsMapper.mapToResponseDTO(analytics);
  }

  public List<AnalyticsResponseDTO> getAllMonthlyAnalytics(Long customerId){
      // Get all analytics rows for this customer (all months / all years in DB)
     List <MonthlyAnalytics> analyticsList = monthlyAnalyticsRepository.findByCustomerId(customerId);



      return analyticsList.stream()
              .map(monthlyAnalyticsMapper::mapToResponseDTO)
              .toList();

  }

  public List<AnalyticsResponseDTO>getMonthlyAnalyticsByYear(Long customerId,int year){
      List<MonthlyAnalytics> analyticsList = monthlyAnalyticsRepository.findByCustomerIdAndYear(customerId ,year);



      return analyticsList.stream().map(monthlyAnalyticsMapper::mapToResponseDTO)
              .toList();
  }

  public void updateAnalyticsFromTransaction(TransactionEvent transactionEvent){
      //Derive month and year from transactionDate
      java.time.Month javaMonth =transactionEvent.getTransactionDate().getMonth();
      int year = transactionEvent.getTransactionDate().getYear();

     Month month=Month.valueOf(javaMonth.name());

      //find existing analytics row or create new one
      MonthlyAnalytics analytics = monthlyAnalyticsRepository
              .findByCustomerIdAndMonthAndYear(transactionEvent
                      .getCustomerId(),month,year)
              .orElseGet(()->MonthlyAnalytics.builder()
                      .customerId(transactionEvent.getCustomerId())
                      .month(month)
                      .year(year)
                      .totalSpent(BigDecimal.ZERO)
                      .totalReceived(BigDecimal.ZERO)
                      .transactionCount(0L)
                      .build()
              );

      //update total based on transaction type
      BigDecimal amount = transactionEvent.getAmount();

      switch (transactionEvent.getTransactionType()) {
          case TRANSFER, WITHDRAWAL, PAYMENT -> {
              analytics.setTotalSpent(analytics.getTotalSpent().add(amount));
          }
          case DEPOSIT -> {
              analytics.setTotalReceived(analytics.getTotalReceived().add(amount));
          }
      }
      //increase Transaction count
      analytics.setTransactionCount(analytics.getTransactionCount()+1);

      //save back to DB
      monthlyAnalyticsRepository.save(analytics);


  }

}

