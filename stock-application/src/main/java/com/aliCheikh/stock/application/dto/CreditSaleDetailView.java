package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Le détail complet d'une vente à crédit : ce qui a été vendu, à qui, par qui, et ce qui reste dû.
 *
 * <p>La liste des créances répond à « qui me doit de l'argent ». Elle ne répond pas à « pourquoi » :
 * sans le détail des produits ni l'historique des encaissements, un solde n'est qu'un chiffre que
 * l'on ne peut ni vérifier ni défendre devant le client. Ce modèle de lecture porte la réponse.</p>
 *
 * <p>{@code amountDue} et {@code settled} restent <b>dérivés</b> du total et des paiements : le
 * solde n'est stocké nulle part, il se recalcule. Une donnée dérivable ne se duplique pas.</p>
 */
public record CreditSaleDetailView(UUID saleId,
                                   LocalDateTime occurredAt,
                                   UUID sellerId,
                                   String sellerName,
                                   UUID customerId,
                                   String customerGivenName,
                                   String customerFatherName,
                                   String customerPhoneNumber,
                                   List<CreditSaleLineView> lines,
                                   Money totalAmount,
                                   Money amountPaid,
                                   Money amountDue,
                                   boolean settled,
                                   List<CreditSalePaymentView> payments) {
}
