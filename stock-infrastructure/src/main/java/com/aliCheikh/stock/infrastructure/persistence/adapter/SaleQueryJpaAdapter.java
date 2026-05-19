package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleLineJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.SaleQueryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

@Repository
public class SaleQueryJpaAdapter implements ListSalesQueryPort {

    private final SaleQueryJpaRepository saleQueryJpaRepository;

    public SaleQueryJpaAdapter(SaleQueryJpaRepository saleQueryJpaRepository) {
        this.saleQueryJpaRepository = Objects.requireNonNull(
                saleQueryJpaRepository,
                "saleQueryJpaRepository cannot be null"
        );
    }

    @Override
    public PageResult<SaleView> findByQuery(ListSalesQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        Page<SaleJpaEntity> page = saleQueryJpaRepository.findAll(
                toSpecification(query),
                PageRequest.of(query.page(), query.size(), toSort(query.sort()))
        );

        return new PageResult<>(
                page.getContent().stream()
                        .map(this::toView)
                        .toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Specification<SaleJpaEntity> toSpecification(ListSalesQuery query) {
        Specification<SaleJpaEntity> specification = Specification.allOf();

        if (query.sellerId() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("soldBy"), query.sellerId().getValue()));
        }

        if (query.from() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), query.from()));
        }

        if (query.to() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), query.to()));
        }

        return specification;
    }

    private Sort toSort(List<String> sortExpressions) {
        if (sortExpressions.isEmpty()) {
            return Sort.by(Sort.Order.desc("occurredAt"));
        }

        return Sort.by(sortExpressions.stream()
                .map(this::toOrder)
                .toList());
    }

    private Sort.Order toOrder(String sortExpression) {
        String[] parts = sortExpression.split(",", 2);
        String property = toJpaProperty(parts[0]);
        Sort.Direction direction = parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return new Sort.Order(direction, property);
    }

    private String toJpaProperty(String apiProperty) {
        return switch (apiProperty) {
            case "saleId" -> "id";
            case "sellerId" -> "soldBy";
            case "createdAt" -> "occurredAt";
            default -> apiProperty;
        };
    }

    private SaleView toView(SaleJpaEntity entity) {
        return new SaleView(
                SaleId.of(entity.getId()),
                UserId.of(entity.getSoldBy()),
                entity.getSaleLines().stream()
                        .sorted(Comparator.comparingInt(SaleLineJpaEntity::getLineNumber))
                        .map(this::toLineView)
                        .toList(),
                Money.create(entity.getTotalAmount(), Currency.getInstance(entity.getTotalCurrency())),
                entity.getOccurredAt()
        );
    }

    private SaleLineDto toLineView(SaleLineJpaEntity line) {
        return new SaleLineDto(
                ProductId.of(line.getProductId()),
                line.getQuantity(),
                Money.create(line.getUnitPriceAmount(), Currency.getInstance(line.getUnitPriceCurrency())),
                Money.create(line.getLineTotalAmount(), Currency.getInstance(line.getLineTotalCurrency()))
        );
    }
}