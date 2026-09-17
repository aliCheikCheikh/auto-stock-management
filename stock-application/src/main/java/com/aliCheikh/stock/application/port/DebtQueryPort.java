package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;

/** Reads outstanding and settled credit sales as flat projections, filtered by status and customer. */
public interface DebtQueryPort {

    /**
     * @return the requested page, oldest first for outstanding debts and most recently settled
     * first for settled debts
     */
    PageResult<DebtView> findByQuery(ListDebtsQuery query);
}
