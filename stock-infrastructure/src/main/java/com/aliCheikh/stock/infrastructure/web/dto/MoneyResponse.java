package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

/** Un montant tel qu'il traverse l'API. */
public record MoneyResponse(String amount,
                            String currency) {

    /**
     * Conversion unique pour toute la couche web.
     *
     * <p>Trois écrans exposent des montants — la liste des créances, le détail d'une créance et
     * l'historique des mouvements. Trois copies de cette conversion finiraient par diverger sur le
     * formatage, et le même montant s'afficherait différemment d'un écran à l'autre.</p>
     *
     * <p>{@code toPlainString()} évite la notation scientifique (2E+5) sur les gros montants.</p>
     *
     * @return {@code null} si le montant est absent — cas documenté par chaque champ concerné
     */
    public static MoneyResponse from(Money money) {
        if (money == null) {
            return null;
        }

        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode());
    }
}
