package com.aliCheikh.stock.domain.model.category.port;

import com.aliCheikh.stock.domain.model.category.Category;

import java.util.List;

public interface CategoryRepository {
    List<Category> findAll();
}
