package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.DashboardQueryPort;
import com.aliCheikh.stock.application.usecase.GetDashboardSummaryUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Currency;

@Configuration
public class DashboardConfiguration {

    @Bean
    public GetDashboardSummaryUseCase getDashboardSummaryUseCase(
            DashboardQueryPort dashboardQueryPort,
            Clock businessClock,
            @Value("${app.business-currency:XAF}") String businessCurrency) {
        return new GetDashboardSummaryUseCase(
                dashboardQueryPort,
                businessClock,
                Currency.getInstance(businessCurrency));
    }
}
