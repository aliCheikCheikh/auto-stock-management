package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockMovementJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Repository
public class StockMovementQueryJpaAdapter implements StockMovementQueryPort {

    private final StockMovementJpaRepository stockMovementJpaRepository;
    private final UserDisplayNameResolver userDisplayNameResolver;
    private final SaleSettlementResolver saleSettlementResolver;

    public StockMovementQueryJpaAdapter(StockMovementJpaRepository stockMovementJpaRepository,
                                        UserDisplayNameResolver userDisplayNameResolver,
                                        SaleSettlementResolver saleSettlementResolver) {
        this.stockMovementJpaRepository = Objects.requireNonNull(
                stockMovementJpaRepository,
                "stockMovementJpaRepository cannot be null"
        );
        this.userDisplayNameResolver = Objects.requireNonNull(
                userDisplayNameResolver,
                "userDisplayNameResolver cannot be null"
        );
        this.saleSettlementResolver = Objects.requireNonNull(
                saleSettlementResolver,
                "saleSettlementResolver cannot be null"
        );
    }

    @Override
    public PageResult<StockMovementView> findByQuery(ListStockMovementsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        Page<StockMovementJpaEntity> page = stockMovementJpaRepository.findAll(
                toSpecification(query),
                PageRequest.of(query.page(), query.size(), toSort(query.sort()))
        );

        // Les noms des auteurs sont résolus en une requête pour toute la page : les chercher
        // ligne par ligne produirait un N+1.
        Map<UUID, String> authorNames = userDisplayNameResolver.resolve(
                page.getContent().stream().map(StockMovementJpaEntity::getPerformedBy).toList());

        // Même parti pour l'état de règlement : une seule requête pour toute la page, afin que
        // l'historique puisse annoncer « payée » ou « à crédit » sans interroger chaque vente.
        Map<UUID, Money> amountsDue = saleSettlementResolver.resolveAmountsDue(
                page.getContent().stream().map(StockMovementJpaEntity::getSaleId).toList());

        return new PageResult<>(
                page.getContent().stream()
                        .map(entity -> toView(entity, authorNames, amountsDue))
                        .toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Specification<StockMovementJpaEntity> toSpecification(ListStockMovementsQuery query) {
        Specification<StockMovementJpaEntity> specification = Specification.allOf();

        if (query.productId() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("productId"), query.productId().getValue()));
        }

        if (query.locationId() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("sourceLocationId"), query.locationId().getValue()),
                            criteriaBuilder.equal(root.get("destinationLocationId"), query.locationId().getValue())
                    ));
        }

        if (query.type() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("movementType"), query.type()));
        }

        return getStockMovementJpaEntitySpecification(specification, query.from(), query.to(), query);
    }

    static Specification<StockMovementJpaEntity> getStockMovementJpaEntitySpecification(Specification<StockMovementJpaEntity> specification, LocalDateTime from, LocalDateTime time, ListStockMovementsQuery query) {
        if (from != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }

        if (time != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), time));
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
            case "movementId" -> "id";
            case "type" -> "movementType";
            case "executedBy" -> "performedBy";
            case "executedAt" -> "occurredAt";
            default -> apiProperty;
        };
    }

    private StockMovementView toView(StockMovementJpaEntity entity,
                                     Map<UUID, String> authorNames,
                                     Map<UUID, Money> amountsDue) {
        UUID locationId = entity.getSourceLocationId() != null
                ? entity.getSourceLocationId()
                : entity.getDestinationLocationId();

        return new StockMovementView(
                MovementId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                LocationId.of(locationId),
                entity.getMovementType() == MovementType.TRANSFER
                        ? LocationId.of(entity.getDestinationLocationId())
                        : null,
                entity.getMovementType(),
                entity.getQuantity(),
                UserId.of(entity.getPerformedBy()),
                authorNames.get(entity.getPerformedBy()),
                entity.getOccurredAt(),
                entity.getSaleId() == null ? null : SaleId.of(entity.getSaleId()),
                OperationId.of(entity.getOperationId()),
                entity.getSaleId() == null ? null : amountsDue.get(entity.getSaleId())
        );
    }
}
