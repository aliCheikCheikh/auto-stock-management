package com.aliCheikh.stock.infrastructure.web.handler;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductNameException;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductReferenceException;
import com.aliCheikh.stock.domain.exception.product.InactiveProductException;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferException;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "product-not-found",
                "Product not found",
                exception.getMessage(),
                "PRODUCT_NOT_FOUND"
        );
    }

    @ExceptionHandler(SaleNotFoundException.class)
    public ProblemDetail handleSaleNotFound(SaleNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "sale-not-found",
                "Sale not found",
                exception.getMessage(),
                "SALE_NOT_FOUND"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String expectedType = exception.getRequiredType() == null
                ? "value"
                : exception.getRequiredType().getSimpleName();

        ProblemDetail problem = validationProblem("Invalid request parameter");
        problem.setProperty("errors", List.of(Map.of(
                "field", exception.getName(),
                "message", "must be a valid " + expectedType,
                "rejectedValue", String.valueOf(exception.getValue())
        )));

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        ProblemDetail problem = validationProblem("Request body validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage()),
                        "rejectedValue", String.valueOf(error.getRejectedValue())
                ))
                .toList());

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return validationProblem("Malformed or missing request body");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = validationProblem("Request validation failed");
        problem.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage(),
                        "rejectedValue", String.valueOf(violation.getInvalidValue())
                ))
                .toList());

        return problem;
    }

    @ExceptionHandler(StorageNotFoundException.class)
    public ProblemDetail handleStorageNotFound(StorageNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "location-not-found",
                "Location not found",
                exception.getMessage(),
                "LOCATION_NOT_FOUND"
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "stock-insufficient",
                "Stock insufficient",
                exception.getMessage(),
                "STOCK_INSUFFICIENT"
        );
    }

    @ExceptionHandler(InactiveProductException.class)
    public ProblemDetail handleInactiveProduct(InactiveProductException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "product-inactive",
                "Product inactive",
                exception.getMessage(),
                "PRODUCT_INACTIVE"
        );
    }

    @ExceptionHandler(InvalidStockTransferException.class)
    public ProblemDetail handleInvalidStockTransfer(InvalidStockTransferException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "invalid-stock-transfer",
                "Invalid stock transfer",
                exception.getMessage(),
                "INVALID_TRANSFER"
        );
    }

    @ExceptionHandler(DuplicateProductReferenceException.class)
    public ProblemDetail handleDuplicateProductReference(DuplicateProductReferenceException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "product-reference-already-used",
                "Product reference already used",
                exception.getMessage(),
                "PRODUCT_REFERENCE_ALREADY_USED"
        );
    }

    @ExceptionHandler(DuplicateProductNameException.class)
    public ProblemDetail handleDuplicateProductName(DuplicateProductNameException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "product-name-already-used",
                "Product name already used",
                exception.getMessage(),
                "PRODUCT_NAME_ALREADY_USED"
        );
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "business-rule-violation",
                "Business rule violation",
                exception.getMessage(),
                "BUSINESS_RULE_VIOLATION"
        );
    }

    private ProblemDetail validationProblem(String detail) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Bad Request",
                detail,
                "VALIDATION_FAILED"
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String type,
            String title,
            String detail,
            String code
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://api.stock.example.com/errors/" + type));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);

        return problem;
    }
}
