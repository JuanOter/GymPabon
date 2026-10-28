package com.gympabon.model;

/**
 * Identification and contact data shared by clients, trainers and system
 * users, so these fields are not repeated in each class.
 */
public abstract class Person {

    private int id;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    protected Person() {
    }

    protected Person(String documentNumber, String firstName, String lastName, String phone, String email) {
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }

    /**
     * Returns the first and last names together, ready to be shown on screens
     * and reports.
     *
     * @return the full name of the person
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return documentNumber + " - " + getFullName();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
