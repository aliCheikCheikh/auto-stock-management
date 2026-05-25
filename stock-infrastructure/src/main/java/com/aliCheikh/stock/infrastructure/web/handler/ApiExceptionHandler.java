package com.aliCheikh.stock.infrastructure.web.handler;

import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("https://api.stock.example.com/errors/product-not-found"));
        problem.setTitle("Product not found");
        problem.setDetail(exception.getMessage());
        problem.setProperty("code", "PRODUCT_NOT_FOUND");

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://api.stock.example.com/errors/validation-failed"));
        problem.setTitle("Bad Request");
        problem.setDetail("Invalid request parameter");
        problem.setProperty("code", "VALIDATION_FAILED");

        return problem;

    }

}
