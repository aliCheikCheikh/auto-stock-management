package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.ProductSearchView;

import java.util.List;

public interface ProductSearchQueryPort {

    List<ProductSearchView> findProductsByKeyword(String keyword, int limit);
}
