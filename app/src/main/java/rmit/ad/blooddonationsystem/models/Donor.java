package rmit.ad.blooddonationsystem.models;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.UUID;


public class Donor {

    public enum Sex {
        MALE,
        FEMALE,
        OTHER
    }

    private String email;
    private String id;
    private String firstName;
    private String lastName;
    private Sex sex;
    private Date dateOfBirth;
    private BloodType bloodType;

    public Donor() {}

    // email/password authentication
    public Donor(String email, String id, String firstName, String lastName, Sex sex, Date dateOfBirth, BloodType bloodType) {
        this.email = email;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.bloodType = bloodType;
    }

    // for proxy donor
    public Donor(String email, String firstName, String lastName, Sex sex, Date dateOfBirth, BloodType bloodType) {
        this.email = email;
        this.id = generateRandomId();
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.bloodType = bloodType;
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString();  // Generates a unique random ID
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Exclude
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Sex getSex() {
        return sex;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public String getEmail() {
        return email;
    }

    // Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }
}
