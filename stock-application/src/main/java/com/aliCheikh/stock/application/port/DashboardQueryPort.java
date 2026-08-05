package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.dashboard.DashboardQueryCriteria;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSnapshot;

public interface DashboardQueryPort {
    DashboardSnapshot load(DashboardQueryCriteria criteria);
}
