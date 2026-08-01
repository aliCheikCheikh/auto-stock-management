package com.aliCheikh.stock.application.dto;

/**
 * Le filtre appliqué à la consultation des créances.
 *
 * <p>Ce type vit dans l'application et non dans le domaine, malgré les apparences. L'état d'une
 * vente est déjà porté par l'agrégat — {@code Sale.isOnCredit()} — et n'a pas besoin d'un second
 * vocabulaire. Ce qui est nommé ici, c'est un <b>critère d'interrogation</b> : la preuve en est
 * {@link #ALL}, qui ne correspond à aucun état dans lequel une vente puisse se trouver.</p>
 */
public enum DebtStatus {

    /** Les créances vivantes : il reste quelque chose à encaisser. */
    OUTSTANDING,

    /**
     * Les créances éteintes.
     *
     * <p>Elles ne disparaissent pas une fois payées : c'est précisément le jour où un client
     * conteste avoir réglé que la trace devient utile.</p>
     */
    SETTLED,

    /** Les deux, pour qui veut lire l'historique d'un client d'un seul tenant. */
    ALL
}
