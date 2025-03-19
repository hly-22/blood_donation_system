package rmit.ad.blooddonationsystem.models;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.UUID;

public class Donation {

    public enum BloodBagSize {
        SMALL(350),
        LARGE(400);

        private final int volume;

        BloodBagSize(int volume) {
            this.volume = volume;
        }

        public int getVolume() {
            return volume;
        }
    }

    private String donationID;      // only to ensure uniqueness
    private String associatedSite;
    private String donorID;
    private String donorFullName;
    private BloodType donorBloodType;
    private Date donationDate;
    private BloodBagSize bloodBagSize;
    private boolean hasDonate;

    public Donation() {}

    // for creation
    public Donation(String donationID, String associatedSite, String donorID, String donorFullName, BloodType donorBloodType) {
        this.donationID = donationID;
        this.associatedSite = associatedSite;
        this.donorID = donorID;
        this.donorFullName = donorFullName;
        this.donorBloodType = donorBloodType;
        this.donationDate = null;
        this.bloodBagSize = null;
        this.hasDonate = false;
    }

    public Donation(String associatedSite, String donorID, String donorFullName, BloodType donorBloodType) {
        this.donationID = generateRandomId();
        this.associatedSite = associatedSite;
        this.donorID = donorID;
        this.donorFullName = donorFullName;
        this.donorBloodType = donorBloodType;
        this.donationDate = null;
        this.bloodBagSize = null;
        this.hasDonate = false;
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString();  // Generates a unique random ID
    }

    // Getters
    public String getDonationID() {
        return donationID;
    }

    public String getAssociatedSite() {
        return associatedSite;
    }

    public String getDonorID() {
        return donorID;
    }

    public String getDonorFullName() {
        return donorFullName;
    }

    public BloodType getDonorBloodType() {
        return donorBloodType;
    }

    public Date getDonationDate() {
        return donationDate;
    }

    public BloodBagSize getBloodBagSize() {
        return bloodBagSize;
    }

    @Exclude
    public int getBloodBagSizeVolume() {
        return bloodBagSize.getVolume();
    }

    public boolean isHasDonate() {
        return hasDonate;
    }

    // Setters
    public void setDonationDate(Date donationDate) {
        this.donationDate = donationDate;
    }

    public void setBloodBagSize(BloodBagSize bloodBagSize) {
        this.bloodBagSize = bloodBagSize;
    }

    public void setHasDonate(boolean hasDonate) {
        this.hasDonate = hasDonate;
    }

}
