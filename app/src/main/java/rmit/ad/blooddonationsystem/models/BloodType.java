package rmit.ad.blooddonationsystem.models;

import androidx.annotation.NonNull;

public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String shortForm;

    BloodType(String shortForm) {
        this.shortForm = shortForm;
    }

    @Override
    public String toString() {
        return shortForm;
    }

    public static BloodType getBloodTypeFromText(String shortForm) {
        switch (shortForm) {
            case "A+":
                return BloodType.A_POSITIVE;
            case "A-":
                return BloodType.A_NEGATIVE;
            case "B+":
                return BloodType.B_POSITIVE;
            case "B-":
                return BloodType.B_NEGATIVE;
            case "AB+":
                return BloodType.AB_POSITIVE;
            case "AB-":
                return BloodType.AB_NEGATIVE;
            case "O+":
                return BloodType.O_POSITIVE;
            case "O-":
                return BloodType.O_NEGATIVE;
            default:
                throw new IllegalArgumentException("Unknown blood type: " + shortForm);
        }
    }
}
