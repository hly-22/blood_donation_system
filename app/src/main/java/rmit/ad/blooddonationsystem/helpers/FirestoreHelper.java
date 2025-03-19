package rmit.ad.blooddonationsystem.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rmit.ad.blooddonationsystem.models.BloodType;
import rmit.ad.blooddonationsystem.models.Donation;
import rmit.ad.blooddonationsystem.models.Site;
import rmit.ad.blooddonationsystem.models.Volunteer;

public class FirestoreHelper {

    private static FirestoreHelper instance;
    private FirebaseFirestore db;

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    public void addUserData(String userID, String userType, Map<String, Object> userMap, Context context) {
        db.collection("users").document(userID)
                .set(userMap)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Added " + userType + " to Firestore", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to add " + userType + " to Firestore", Toast.LENGTH_SHORT).show());
    }

    public void getUserData(String userID, OnSuccessListener<DocumentSnapshot> onSuccessListener, OnFailureListener onFailureListener) {
        db.collection("users")
                .document(userID)
                .get()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }

    public void getDonorsBySite(String siteId, FirestoreCallback<List<Donation>> callback) {
        // Assuming you have a 'donations' collection where donor information is stored
        db.collection("donations")
                .whereEqualTo("associatedSite", siteId) // Filter by site ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Donation> donations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Donation donation = document.toObject(Donation.class);
                            donations.add(donation);
                        }
                        callback.onSuccess(donations);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getDonationsBySite(String siteId, FirestoreCallback<List<Donation>> callback) {
        // Assuming you have a 'donations' collection where donor information is stored
        db.collection("donations")
                .whereEqualTo("associatedSite", siteId)
                .whereEqualTo("hasDonate", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Donation> donations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Donation donation = document.toObject(Donation.class);
                            donations.add(donation);
                        }
                        callback.onSuccess(donations);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getSitesByManager(String managerUID, FirestoreCallback<List<Site>> callback) {
        db.collection("sites")
                .whereEqualTo("associatedSiteManager", managerUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Site> siteList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Site site = document.toObject(Site.class);
                        List<BloodType> requiredBloodTypes = new ArrayList<>();
                        if (site.getRequiredBloodTypesString() == null) {
                            requiredBloodTypes = null;
                        } else {
                            for (String bloodTypeStr : site.getRequiredBloodTypesString()) {
                                requiredBloodTypes.add(BloodType.valueOf(bloodTypeStr));
                            }
                        };
                        siteList.add(site);
                    }
                    callback.onSuccess(siteList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public void addSiteDatas(Site site) {
        db.collection("sites").document(site.getId())
                .set(site)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Site added");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to add site: " + e.getMessage()));
    }

    public void getDonationSitesInBounds(LatLngBounds bounds, FirestoreCallback<List<Site>> callback) {
        // Define boundaries
        double north = bounds.northeast.latitude;
        double south = bounds.southwest.latitude;
        double east = bounds.northeast.longitude;
        double west = bounds.southwest.longitude;

        // Query Firestore for sites within the bounds
        db.collection("sites")
                .whereGreaterThanOrEqualTo("latitude", south)
                .whereLessThanOrEqualTo("latitude", north)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Site> sitesInBounds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Site site = document.toObject(Site.class);

                            // Further filter by longitude
                            if (site.getLongitude() >= west && site.getLongitude() <= east) {
                                sitesInBounds.add(site);
                            }
                        }
                        callback.onSuccess(sitesInBounds);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void updateSiteData(Site updatedSite, FirestoreCallback<Void> callback) {
        db.collection("sites").document(updatedSite.getId())
                .set(updatedSite)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Site successfully updated!");
                    // Call the callback's onSuccess method to notify the activity
                    callback.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating site", e);
                    callback.onFailure(e); // Optionally, pass the error to callback
                });
    }

    public void getAllSites(FirestoreCallback<List<Site>> callback) {
        db.collection("sites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Site> sites = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Site site = document.toObject(Site.class);
                            sites.add(site);
                        }
                        callback.onSuccess(sites);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void addVolunteer(Volunteer volunteer, FirestoreCallback<Void> callback) {
        db.collection("volunteers")
                .add(volunteer)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }


    public void getVolunteersBySite(String siteId, FirestoreCallback<List<Volunteer>> callback) {
        db.collection("volunteers")
                .whereEqualTo("associatedSite", siteId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Volunteer> volunteers = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Volunteer volunteer = document.toObject(Volunteer.class);
                        volunteers.add(volunteer);
                    }
                    callback.onSuccess(volunteers);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteVolunteer(String email, FirestoreCallback<Boolean> callback) {
        db.collection("volunteers")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Assuming email is unique, delete the first matching document
                        String documentId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("volunteers").document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        callback.onFailure(new Exception("Volunteer not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addDonationData(Donation donation, FirestoreCallback<Void> callback) {
        db.collection("donations").document(donation.getDonationID())
                .set(donation)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getDonationsByDonorId(String donorId, FirestoreCallback<List<Donation>> callback) {
        db.collection("donations")
                .whereEqualTo("donorID", donorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Donation> donations = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Donation donation = document.toObject(Donation.class);
                        donations.add(donation);
                    }
                    callback.onSuccess(donations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateDonation(Donation donation, FirestoreCallback<Void> callback) {
        if (donation == null || donation.getDonationID() == null) {
            callback.onFailure(new IllegalArgumentException("Donation or DonationID cannot be null"));
            return;
        }

        // Update the donation document in Firestore
        db.collection("donations").document(donation.getDonationID())
                .set(donation)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }



}
