package rmit.ad.blooddonationsystem.models;

import com.google.firebase.firestore.Exclude;

public class Volunteer {

    private String email;
    private String id;
    private String firstName;
    private String lastName;
    private String associatedSite;      // sID of associated site

    public Volunteer() {}

    public Volunteer(String email, String id, String firstName, String lastName, String associatedSite) {
        this.email = email;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.associatedSite = associatedSite;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAssociatedSite() {
        return associatedSite;
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

    public void setEmail(String email) {
        this.email = email;
    }
}
