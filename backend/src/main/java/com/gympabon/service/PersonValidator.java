package com.gympabon.service;

import com.gympabon.exception.BusinessRuleException;
import com.gympabon.model.Person;
import java.util.regex.Pattern;

/**
 * Validates the identification and contact data shared by clients, trainers
 * and users, so the three services apply the same rules.
 */
final class PersonValidator {

    private static final Pattern DOCUMENT = Pattern.compile("\\d{6,12}");
    private static final Pattern PHONE = Pattern.compile("[0-9 +]{7,20}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MAX_NAME_LENGTH = 60;

    private PersonValidator() {
    }

    /**
     * Checks the person data and removes extra spaces from the names.
     *
     * @param person person to validate
     * @throws BusinessRuleException if a required field is missing or has an invalid format
     */
    static void validate(Person person) {
        if (person == null) {
            throw new BusinessRuleException("Los datos de la persona son obligatorios");
        }
        String document = trim(person.getDocumentNumber());
        if (document == null || !DOCUMENT.matcher(document).matches()) {
            throw new BusinessRuleException("El documento debe tener entre 6 y 12 dígitos");
        }
        person.setDocumentNumber(document);
        person.setFirstName(requireName(person.getFirstName(), "Los nombres"));
        person.setLastName(requireName(person.getLastName(), "Los apellidos"));

        String phone = trim(person.getPhone());
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new BusinessRuleException("El teléfono solo puede tener números y espacios");
        }
        person.setPhone(phone);

        String email = trim(person.getEmail());
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new BusinessRuleException("El correo electrónico no es válido");
        }
        person.setEmail(email);
    }

    /**
     * Checks that a text is filled in and returns it without extra spaces.
     *
     * @param value text to check
     * @param field name of the field, used in the error message
     * @return the trimmed text
     * @throws BusinessRuleException if the text is empty
     */
    static String requireText(String value, String field) {
        String text = trim(value);
        if (text == null) {
            throw new BusinessRuleException(field + " es obligatorio");
        }
        return text;
    }

    private static String requireName(String value, String field) {
        String name = trim(value);
        if (name == null) {
            throw new BusinessRuleException(field + " son obligatorios");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException(field + " no pueden tener más de " + MAX_NAME_LENGTH + " caracteres");
        }
        return name.replaceAll("\\s+", " ");
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
