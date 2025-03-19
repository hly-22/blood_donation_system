package rmit.ad.blooddonationsystem.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Site {

    private String id;
    private String associatedSiteManager;
    private String locationName;
    private double latitude;
    private double longitude;
    private String openingHours;
    private List<BloodType> requiredBloodTypes;
    private boolean isActive;
    private List<String> requiredBloodTypesString;

    public Site() {}

    // site
    public Site(String id, String associatedSiteManager, String locationName, double latitude, double longitude, String openingHours, List<BloodType> requiredBloodTypes, boolean isActive) {
        this.id = id;
        this.associatedSiteManager = associatedSiteManager;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
        this.requiredBloodTypes = requiredBloodTypes;
        this.isActive = isActive;
    }

    public Site(String associatedSiteManager, String locationName, double latitude, double longitude, String openingHours, List<BloodType> requiredBloodTypes, boolean isActive) {
        this.id = generateRandomId();
        this.associatedSiteManager = associatedSiteManager;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
        this.requiredBloodTypes = requiredBloodTypes;
        this.isActive = isActive;
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString();  // Generates a unique random ID
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAssociatedSiteManager() {
        return associatedSiteManager;
    }

    public String getLocationName() {
        return locationName;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    @Exclude
    public List<BloodType> getRequiredBloodTypes() {
        if (requiredBloodTypesString != null) {
            List<BloodType> enumList = new ArrayList<>();
            for (String bloodType : requiredBloodTypesString) {
                enumList.add(BloodType.valueOf(bloodType));
            }
            this.requiredBloodTypes = enumList;
        }
        return requiredBloodTypes;
    }

    public List<String> getRequiredBloodTypesString() {
        if (requiredBloodTypes == null) {
            return requiredBloodTypesString;
        }
        return convertBloodTypesToStringList();
    }

    public boolean isActive() {
        return isActive;
    }

    @Exclude
    public List<String> convertBloodTypesToStringList() {
        if (requiredBloodTypes == null || requiredBloodTypes.isEmpty()) {
            return null;
        }

        List<String> stringList = new ArrayList<>();
        for (BloodType bloodType : requiredBloodTypes) {
            stringList.add(bloodType.name());
        }
        return stringList;
    }

    @Exclude
    public String convertBloodTypesToString() {
        if (requiredBloodTypes == null || requiredBloodTypes.isEmpty()) {
            return "None"; // Return an empty string if the list is null or empty
        }

        // Join the list of BloodType enums into a single string, separated by commas
        return requiredBloodTypes.stream()
                .map(BloodType::toString) // Convert each BloodType enum to its short form (e.g., "A+")
                .collect(Collectors.joining("   ")); // Join the strings with a comma and space
    }

    // Setters
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setAssociatedSiteManager(String associatedSiteManager) {
        this.associatedSiteManager = associatedSiteManager;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public void setRequiredBloodTypes(List<BloodType> requiredBloodTypes) {
        this.requiredBloodTypes = requiredBloodTypes;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
