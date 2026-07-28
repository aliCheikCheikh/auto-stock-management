package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerGivenNameException;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Client du magasin, à qui une vente à crédit peut être accordée.
 *
 * <p><b>Modèle de nommage.</b> Au Tchad, une personne est désignée par son nom propre, suivi du
 * nom de son père. Il n'existe pas de « nom de famille » au sens patronyme partagé par une lignée :
 * plaquer un modèle {@code firstName}/{@code lastName} conduirait à stocker le nom du père dans un
 * champ censé porter le patronyme, et à inverser les deux au moindre doute. D'où les deux champs
 * nommés d'après le métier :</p>
 * <ul>
 *     <li>{@code givenName} — le nom propre de la personne, <b>obligatoire</b> : c'est par lui
 *         qu'on l'appelle et qu'on la reconnaît dans la liste des créances ;</li>
 *     <li>{@code fatherName} — le nom du père, <b>optionnel</b> : il sert à distinguer deux
 *         clients portant le même nom propre.</li>
 * </ul>
 *
 * <p><b>Identité.</b> L'identité technique est le {@link CustomerId}, seul support de l'égalité.
 * L'identifiant naturel est le {@link PhoneNumber}, obligatoire, qui sert aussi de canal de
 * relance. L'email reste optionnel : beaucoup de clients n'en ont pas.</p>
 *
 * <p><b>Hors périmètre de cet agrégat :</b> l'unicité du téléphone ou de l'email. Un agrégat ne
 * connaît que lui-même ; l'unicité est garantie par une contrainte en base et vérifiée via le
 * repository.</p>
 */
public final class Customer {

    /** Contrôle volontairement permissif : on attrape les fautes de frappe, pas la RFC 5322. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerId customerId;
    private final PhoneNumber phoneNumber;
    private final String givenName;
    private final String fatherName;
    private final String email;

    /**
     * Porte les invariants de l'agrégat : tout chemin de construction, présent ou futur,
     * passe obligatoirement par ici.
     */
    private Customer(CustomerId customerId,
                     PhoneNumber phoneNumber,
                     String givenName,
                     String fatherName,
                     String email) {
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
        this.givenName = requireUsableGivenName(givenName);
        this.fatherName = normalizeOptionalText(fatherName);
        this.email = normalizeOptionalEmail(email);
    }

    /**
     * Crée un nouveau client.
     *
     * @param customerId  identité technique, jamais nulle
     * @param phoneNumber identifiant naturel et canal de relance, jamais nul
     * @param givenName   nom propre de la personne, obligatoire
     * @param fatherName  nom du père, facultatif ({@code null} accepté)
     * @param email       adresse email, facultative ({@code null} accepté), validée si présente
     */
    public static Customer create(CustomerId customerId,
                                  PhoneNumber phoneNumber,
                                  String givenName,
                                  String fatherName,
                                  String email) {
        return new Customer(customerId, phoneNumber, givenName, fatherName, email);
    }

    private static String requireUsableGivenName(String givenName) {
        if (givenName == null || givenName.isBlank()) {
            throw new InvalidCustomerGivenNameException(givenName);
        }
        return givenName.trim();
    }

    /** Un texte optionnel absent ou vide est ramené à {@code null} : une seule façon de dire « absent ». */
    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Normalise l'email en minuscules avant de le valider : sans cette forme canonique, la règle
     * « email unique s'il est présent » ne tiendrait pas ({@code A@B.com} et {@code a@b.com}
     * seraient deux valeurs distinctes).
     */
    private static String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidCustomerEmailException(email);
        }
        return normalizedEmail;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    /** Le nom propre du client, toujours présent. */
    public String getGivenName() {
        return givenName;
    }

    /** Le nom du père, s'il a été renseigné. */
    public Optional<String> getFatherName() {
        return Optional.ofNullable(fatherName);
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /** Deux clients sont le même client s'ils partagent la même identité — pas les mêmes données. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer other = (Customer) o;
        return customerId.equals(other.customerId);
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }

    @Override
    public String toString() {
        return "Customer{" + customerId + ", " + givenName + ", " + phoneNumber + "}";
    }
}
