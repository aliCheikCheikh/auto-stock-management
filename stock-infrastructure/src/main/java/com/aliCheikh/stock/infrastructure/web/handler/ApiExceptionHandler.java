package com.aliCheikh.stock.infrastructure.web.handler;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.exception.category.CategoryInUseException;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.exception.customer.CustomerNotFoundException;
import com.aliCheikh.stock.domain.exception.customer.DuplicateCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.exception.customer.InvalidPhoneNumberException;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductNameException;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductReferenceException;
import com.aliCheikh.stock.domain.exception.product.InactiveProductException;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.exception.sale.CreditSaleRequiresCustomerException;
import com.aliCheikh.stock.domain.exception.sale.PaymentExceedsAmountDueException;
import com.aliCheikh.stock.domain.exception.sale.SaleAlreadySettledException;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferException;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.exception.user.DuplicateUserEmailException;
import com.aliCheikh.stock.domain.exception.user.IncorrectCurrentPasswordException;
import com.aliCheikh.stock.domain.exception.user.InvalidPasswordException;
import com.aliCheikh.stock.domain.exception.user.InvalidUserEmailException;
import com.aliCheikh.stock.domain.exception.user.InvalidUserNameException;
import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.exception.user.OwnerPasswordResetNotAllowedException;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
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
                        "rejectedValue", safeRejectedValue(error.getField(), error.getRejectedValue())
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
                        "rejectedValue", safeRejectedValue(
                                violation.getPropertyPath().toString(), violation.getInvalidValue())
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

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "customer-not-found",
                "Customer not found",
                exception.getMessage(),
                "CUSTOMER_NOT_FOUND"
        );
    }

    @ExceptionHandler(DuplicatePhoneNumberException.class)
    public ProblemDetail handleDuplicatePhoneNumber(DuplicatePhoneNumberException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "customer-phone-already-used",
                "Phone number already used",
                exception.getMessage(),
                "CUSTOMER_PHONE_ALREADY_USED"
        );
    }

    @ExceptionHandler(DuplicateCustomerEmailException.class)
    public ProblemDetail handleDuplicateCustomerEmail(DuplicateCustomerEmailException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "customer-email-already-used",
                "Email already used",
                exception.getMessage(),
                "CUSTOMER_EMAIL_ALREADY_USED"
        );
    }

    @ExceptionHandler(CreditSaleRequiresCustomerException.class)
    public ProblemDetail handleCreditSaleRequiresCustomer(CreditSaleRequiresCustomerException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "credit-sale-requires-customer",
                "Credit sale requires a customer",
                exception.getMessage(),
                "CREDIT_SALE_REQUIRES_CUSTOMER"
        );
    }

    @ExceptionHandler(InvalidPhoneNumberException.class)
    public ProblemDetail handleInvalidPhoneNumber(InvalidPhoneNumberException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-phone-number",
                "Invalid phone number",
                exception.getMessage(),
                "INVALID_PHONE_NUMBER"
        );
    }

    @ExceptionHandler(SaleAlreadySettledException.class)
    public ProblemDetail handleSaleAlreadySettled(SaleAlreadySettledException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "sale-already-settled",
                "Sale already settled",
                exception.getMessage(),
                "SALE_ALREADY_SETTLED"
        );
    }

    @ExceptionHandler(PaymentExceedsAmountDueException.class)
    public ProblemDetail handlePaymentExceedsAmountDue(PaymentExceedsAmountDueException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "payment-exceeds-amount-due",
                "Payment exceeds the outstanding balance",
                exception.getMessage(),
                "PAYMENT_EXCEEDS_AMOUNT_DUE"
        );
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "category-not-found",
                "Category not found",
                exception.getMessage(),
                "CATEGORY_NOT_FOUND"
        );
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ProblemDetail handleDuplicateCategoryName(DuplicateCategoryNameException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "category-name-already-used",
                "Category name already used",
                exception.getMessage(),
                "CATEGORY_NAME_ALREADY_USED"
        );
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ProblemDetail handleCategoryInUse(CategoryInUseException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "category-in-use",
                "Category still holds products",
                exception.getMessage(),
                "CATEGORY_IN_USE"
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "user-not-found",
                "Utilisateur introuvable",
                exception.getMessage(),
                "USER_NOT_FOUND"
        );
    }

    @ExceptionHandler(DuplicateUserEmailException.class)
    public ProblemDetail handleDuplicateUserEmail(DuplicateUserEmailException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "user-email-already-used",
                "Email déjà utilisé",
                exception.getMessage(),
                "USER_EMAIL_ALREADY_USED"
        );
    }

    @ExceptionHandler(LastActiveOwnerException.class)
    public ProblemDetail handleLastActiveOwner(LastActiveOwnerException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "last-active-owner",
                "Dernier propriétaire actif",
                exception.getMessage(),
                "LAST_ACTIVE_OWNER"
        );
    }

    @ExceptionHandler(OwnerPasswordResetNotAllowedException.class)
    public ProblemDetail handleOwnerPasswordReset(OwnerPasswordResetNotAllowedException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "owner-password-reset-forbidden",
                "Réinitialisation indisponible",
                exception.getMessage(),
                "OWNER_PASSWORD_RESET_FORBIDDEN"
        );
    }

    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ProblemDetail handleIncorrectCurrentPassword(IncorrectCurrentPasswordException exception) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "incorrect-current-password",
                "Mot de passe actuel incorrect",
                exception.getMessage(),
                "CURRENT_PASSWORD_INCORRECT"
        );
    }

    @ExceptionHandler({InvalidUserEmailException.class, InvalidUserNameException.class, InvalidPasswordException.class})
    public ProblemDetail handleInvalidUserData(DomainException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-user-data",
                "Données utilisateur invalides",
                exception.getMessage(),
                "INVALID_USER_DATA"
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

    private String safeRejectedValue(String field, Object rejectedValue) {
        if (field.toLowerCase().contains("password")) {
            return "[REDACTED]";
        }
        return String.valueOf(rejectedValue);
    }
}
