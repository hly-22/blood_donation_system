package rmit.ad.blooddonationsystem.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.AuthHelper;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;
import rmit.ad.blooddonationsystem.models.BloodType;
import rmit.ad.blooddonationsystem.models.Donation;
import rmit.ad.blooddonationsystem.models.Donor;
import rmit.ad.blooddonationsystem.models.Site;

public class DonorActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private GoogleMap googleMap;
    private FusedLocationProviderClient client;
    private Marker selectedMarker;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private Site selectedSite;
    private Donor currentUser;
    private Polyline routePolyline;
    private List<Donation> donationList;
    private List<BloodType> selectedBloodTypesEnum = new ArrayList<>();
    private List<String> selectedBloodTypes = new ArrayList<>();
    private List<Marker> siteMarkers = new ArrayList<>(); // List to store active site markers

    private Date dateOfBirth;
    private BloodType selectedBloodType;
    private Donor.Sex selectedSex;

    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();
    AuthHelper authHelper = AuthHelper.getInstance();

    LinearLayout viewSiteDonor, proxyDonorForm;
    TextView siteName, siteAddress, siteOpeningHours, requiredBloodTypes;
    Button btnFindRoute, btnRegisterOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor);

        client = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.donorMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        firestoreHelper.getUserData(authHelper.getCurrentUser().getUid(), new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                currentUser = documentSnapshot.toObject(Donor.class);
                Log.d("Firestore", currentUser.getFullName() + " is the current user.");
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Firestore", "User does not exist.");
            }
        });

        FloatingActionButton fabSelectBloodTypes = findViewById(R.id.fab_select_blood_types);
        fabSelectBloodTypes.setOnClickListener(view -> showBloodTypeSelectionDialog());


        viewSiteDonor = findViewById(R.id.viewSiteDonor);
        proxyDonorForm = findViewById(R.id.proxyDonorForm);
        btnFindRoute = findViewById(R.id.btnFindRoute);
        btnRegisterOther = findViewById(R.id.btnRegisterOther);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.setOnCameraIdleListener(() -> {
            LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
            fetchDonationSites(bounds); // Fetch sites based on the current camera bounds
        });

        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, show the user's location on the map
            googleMap.setMyLocationEnabled(true);
//            getUserLocation();
            getCurrentLocationAndDisplaySites();
        } else {
            // Permission not granted, request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get user location
//                getUserLocation();
                getCurrentLocationAndDisplaySites();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Location permission is required to show your current location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getDonationList() {
        firestoreHelper.getDonationsByDonorId(authHelper.getCurrentUser().getUid(), new FirestoreHelper.FirestoreCallback<List<Donation>>() {
            @Override
            public void onSuccess(List<Donation> result) {
                donationList = result;
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("donations", "Cannot get donations from Firestore.");
            }
        });
    }

    public void showBloodTypeSelectionDialog() {
        final List<BloodType> allBloodTypes = Arrays.asList(BloodType.values());
        List<String> bloodTypeStrings = new ArrayList<>();
        for (BloodType bloodType : allBloodTypes) {
            bloodTypeStrings.add(bloodType.toString());
        }

        boolean[] checkedItems = new boolean[bloodTypeStrings.size()];
        for (int i = 0; i < bloodTypeStrings.size(); i++) {
            checkedItems[i] = selectedBloodTypes.contains(bloodTypeStrings.get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Blood Types")
                .setMultiChoiceItems(bloodTypeStrings.toArray(new CharSequence[0]), checkedItems,
                        (dialog, which, isChecked) -> {
                            String selectedBloodType = bloodTypeStrings.get(which);
                            if (isChecked) {
                                selectedBloodTypes.add(selectedBloodType);
                            } else {
                                selectedBloodTypes.remove(selectedBloodType);
                            }
                        })
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedBloodTypesEnum.clear();
                    for (String selectedBloodType : selectedBloodTypes) {
                        selectedBloodTypesEnum.add(BloodType.getBloodTypeFromText(selectedBloodType));
                    }
                    fetchDonationSites(googleMap.getProjection().getVisibleRegion().latLngBounds);
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    public void onBtnRegisterSelf(View view) {

        AlertDialog dialog = new AlertDialog.Builder(DonorActivity.this).create();
        dialog.setTitle("Register donation at " + selectedSite.getLocationName());
        dialog.setMessage("Do you want to register at this location?");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                "I confirm",
                (dialog1, which) -> {
                    Donation donation = new Donation(selectedSite.getId(), currentUser.getId(), currentUser.getFullName(), currentUser.getBloodType());
                    firestoreHelper.addDonationData(donation, new FirestoreHelper.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d("Firestore", "Donation added.");

                            fetchDonationSites(googleMap.getProjection().getVisibleRegion().latLngBounds);
                            viewSiteDonor.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("Firestore", "Failed to add donation: " + e.getMessage());
                        }
                    });
                });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog12, which) -> {
        });;
        dialog.show();
    }
    public void onBtnRegisterOther(View view) {
        proxyDonorForm.setVisibility(View.VISIBLE);
        viewSiteDonor.setVisibility(View.INVISIBLE);

        handleProxy();
    }

    private void getCurrentLocationAndDisplaySites() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            client.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Move camera to the current location
                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation, 16);
                                googleMap.moveCamera(cameraUpdate);

                                // Fetch nearby donation sites based on current location
                                fetchDonationSites(googleMap.getProjection().getVisibleRegion().latLngBounds);

                            }
                        }
                    });
        } else {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void fetchDonationSites(LatLngBounds bounds) {
        firestoreHelper.getDonationSitesInBounds(bounds, new FirestoreHelper.FirestoreCallback<List<Site>>() {
            @Override
            public void onSuccess(List<Site> result) {

                for (Marker marker : siteMarkers) {
                    marker.remove();  // Remove marker from the map
                }
                siteMarkers.clear();


                getDonationList();

                // If no blood type filter is selected, display all sites within bounds
                List<Site> filteredSites = new ArrayList<>();
                if (selectedBloodTypesEnum.isEmpty()) {
                    // If no filter, include all sites in bounds
                    filteredSites.addAll(result);
                } else {
                    // Otherwise, filter sites based on selected blood types
                    for (Site site : result) {
                        if (site.isActive() && matchesBloodTypes(site, selectedBloodTypesEnum)) {
                            filteredSites.add(site);
                        }
                    }
                }

                for (Site site : filteredSites) {
                    if (site.isActive()) {
                        LatLng siteLocation = new LatLng(site.getLatitude(), site.getLongitude());

                        boolean isRegistered = false;
                        if (donationList != null) {
                            for (Donation donation : donationList) {
                                if (site.getId().equals(donation.getAssociatedSite())) {
                                    isRegistered = true;
                                    break;
                                }
                            }
                        }

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(siteLocation)
                                .title(site.getLocationName())
                                .snippet(site.getOpeningHours());

                        if (isRegistered) {
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                            Log.d("isRegistered", "true at marker");
                        }

                        Marker gMarker = googleMap.addMarker(markerOptions);
                        gMarker.setTag(site);
                        siteMarkers.add(gMarker);

                        googleMap.setOnMarkerClickListener(marker -> {
                            Object tag = marker.getTag();
                            if (tag instanceof Site) {
                                Site selectedSite = (Site) tag;

                                selectedMarker = marker;

                                // Recalculate if the user is registered for this site
                                boolean isRegisteredForThisSite = false;
                                if (donationList != null) {
                                    for (Donation donation : donationList) {
                                        if (selectedSite.getId().equals(donation.getAssociatedSite())) {
                                            isRegisteredForThisSite = true;
                                            break;
                                        }
                                    }
                                }
                                showSiteInfo(selectedSite, isRegisteredForThisSite);  // Update UI
                            } else {
                                Log.e("DonorActivity", "Marker tag is not a Site");
                            }
                            return false;  // Return false so that the default behavior (showing the info window) isn't blocked
                        });

                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DonorActivity", "Error fetching donation sites", e);
            }
        });
    }

    private void showSiteInfo(Site site, boolean isRegistered) {
        if (isRegistered) {
            Log.d("isRegistered", "true at start of showSiteInfo");
        }
        siteName = findViewById(R.id.siteNameViewDonor);
        siteName.setText(site.getLocationName());

        siteOpeningHours = findViewById(R.id.openingHoursViewDonor);
        siteOpeningHours.setText(site.getOpeningHours() != null ? site.getOpeningHours() : "Not available");

        requiredBloodTypes = findViewById(R.id.requiredBTViewDonor);
        site.getRequiredBloodTypes();
        requiredBloodTypes.setText(site.convertBloodTypesToString());

        siteAddress = findViewById(R.id.addressViewDonor);
        LatLng siteLocation = new LatLng(site.getLatitude(), site.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(siteLocation.latitude, siteLocation.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String addressLine = address.getAddressLine(0);
                String city = address.getLocality();
                String country = address.getCountryName();

                String formattedAddress = (addressLine != null && !addressLine.isEmpty())
                        ? addressLine
                        : String.format("%s%s", city != null ? city : "", country != null ? ", " + country : "");

                siteAddress.setText(formattedAddress);
                this.selectedSite = site;
            } else {
                // Handle case where no address is found
                siteAddress.setText("Address not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors, e.g., network issues
            siteAddress.setText("Failed to get address.");
        }


        Button btnRegisterSelf = findViewById(R.id.btnRegisterSelf);

        btnRegisterSelf.setEnabled(true);
        btnRegisterSelf.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red_primary)));  // Default color
        btnRegisterSelf.setText("Register to Donate");  // Default text

        if (isRegistered) {
            // Set button to "Already Registered" if the donor is registered
            btnRegisterSelf.setText("Already Registered");
            btnRegisterSelf.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));  // Gray background
            btnRegisterSelf.setEnabled(false);  // Disable the button
        }

        viewSiteDonor.setVisibility(View.VISIBLE);

    }

    private void handleProxy() {
        // date of birth
        TextView dateOfBirthProxy = findViewById(R.id.dateOfBirthProxy);
        dateOfBirthProxy.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, day1) -> {
                // Format selected date to YYYY-MM-DD
                String formattedMonth = String.format("%02d", month1 + 1); // +1 to account for 0-based month index
                String formattedDay = String.format("%02d", day1);
                String selectedDate = year1 + "-" + formattedMonth + "-" + formattedDay;

                dateOfBirthProxy.setText(selectedDate);

                // Convert selected date to Date object
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    dateOfBirth = sdf.parse(selectedDate); // Converting string to Date
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }, year, month, day);

            datePickerDialog.show();
        });


        // Blood type
        Spinner bloodTypeSpinner = findViewById(R.id.bloodTypeSpinnerProxy);

        // Set up the ArrayAdapter with BloodType enum
        ArrayAdapter<BloodType> adapter = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, BloodType.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodTypeSpinner.setAdapter(adapter);

        bloodTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Update the selectedBloodType whenever the user changes the selection
                selectedBloodType = (BloodType) parentView.getItemAtPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle the case where no blood type is selected (optional)
            }
        });
    }

    public void onBtnProxy(View view) {

        EditText userEmailEdit = findViewById(R.id.emailProxy);
        String userEmail = userEmailEdit.getText().toString();
        // names
        EditText firstNameEditText = findViewById(R.id.firstNameProxy);
        String firstName = firstNameEditText.getText().toString();

        EditText lastNameEditText = findViewById(R.id.lastNameProxy);
        String lastName = lastNameEditText.getText().toString();

        // sex
        RadioGroup sexRadioGroup = findViewById(R.id.sexRadioGroupProxy);
        int selectedId = sexRadioGroup.getCheckedRadioButtonId();

        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            String selectedSexString = selectedRadioButton.getText().toString();

            // Convert the string to the Sex enum type
            try {
                selectedSex = Donor.Sex.valueOf(selectedSexString.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Handle the case when the string does not match any enum value
                Log.e("SexSelection", "Invalid sex value selected: " + selectedSexString);
            }
        }

        if (firstName.isEmpty() || lastName.isEmpty() || selectedSex == null || dateOfBirth == null || selectedBloodType == null ) {
            Toast.makeText(DonorActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
        } else {
            Donor donor = new Donor(userEmail, firstName, lastName, selectedSex, dateOfBirth, selectedBloodType);
            addUserToFirestore(donor.getId(), donor, "donor");

            Donation newDonation = new Donation(selectedSite.getId(), donor.getId(), donor.getFullName(), donor.getBloodType());
            firestoreHelper.addDonationData(newDonation, new FirestoreHelper.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(DonorActivity.this, donor.getFullName() + " has been registered to donate at " + selectedSite.getLocationName(), Toast.LENGTH_LONG).show();

                    proxyDonorForm.setVisibility(View.GONE);
                    viewSiteDonor.setVisibility(View.INVISIBLE);
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(DonorActivity.this, "Failed to add donation", Toast.LENGTH_SHORT).show();
                    Log.e("ProxyDonation", "Failed to add donation for " + donor.getFirstName() + ": " + e.getMessage());
                }
            });
        }
    }

    public void onBtnFindRoute(View view) {
        client.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng siteLocation = new LatLng(selectedSite.getLatitude(), selectedSite.getLongitude());

                            // Fetch the route and draw it on the map
                            fetchRoute(userLocation, siteLocation);
                        } else {
                            Toast.makeText(DonorActivity.this, "Unable to get your current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                ).addOnFailureListener(e -> {
                    Toast.makeText(DonorActivity.this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchRoute(LatLng origin, LatLng destination) {
        String apiKey = getString(R.string.api_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String encodedPolyline = overviewPolyline.getString("points");
                            drawRoute(encodedPolyline);
                        } else {
                            Toast.makeText(this, "No routes found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing route data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error fetching route: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(jsonObjectRequest);
    }

    @SuppressLint("PotentialBehaviorOverride")
    private void drawRoute(String encodedPolyline) {
        List<LatLng> points = PolyUtil.decode(encodedPolyline);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(10);

        // If the route already exists, remove it first
        if (routePolyline != null) {
            routePolyline.remove();
        }

        routePolyline = googleMap.addPolyline(polylineOptions);

        // Add a marker at the destination point
        LatLng destination = points.get(points.size() - 1); // The last point is the destination
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Click to open Google Maps")
        );

        // Set a click listener for the marker to open Google Maps
        marker.setTag(destination); // Store the destination in the marker tag
        googleMap.setOnMarkerClickListener(marker1 -> {
            LatLng destinationLatLng = (LatLng) marker1.getTag();
            if (destinationLatLng != null) {
                openGoogleMapsForNavigation(destinationLatLng);
            }
            return false;
        });
    }

    private void openGoogleMapsForNavigation(LatLng destination) {
        String uri = "google.navigation:q=" + destination.latitude + "," + destination.longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }



    private <T> void addUserToFirestore(String id, T user, String userType) {

        Map<String, Object> userMap = new HashMap<>();
        if (user instanceof Donor) {
            userMap.put("email", ((Donor) user).getEmail());
            userMap.put("id", ((Donor) user).getId());
            userMap.put("firstName", ((Donor) user).getFirstName());
            userMap.put("lastName", ((Donor) user).getLastName());
            userMap.put("sex", ((Donor) user).getSex());
            userMap.put("dateOfBirth", ((Donor) user).getDateOfBirth());
            userMap.put("bloodType", ((Donor) user).getBloodType());
        }

        userMap.put("userType", userType);

        // Add to Firestore
        firestoreHelper.addUserData(id, userType, userMap, DonorActivity.this);
    }

    public void onBtnSignOut(View view) {

        FirebaseAuth.getInstance().signOut();

//        // Clear any user-related data if needed (optional) - later for FCM
//        clearUserData();

        Intent intent = new Intent(DonorActivity.this, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    public void onBtnCloseDonor(View view) {
        proxyDonorForm.setVisibility(View.INVISIBLE);
    }

    private boolean matchesBloodTypes(Site site, List<BloodType> selectedBloodTypes) {
        List<BloodType> requiredBloodTypes = site.getRequiredBloodTypes();
        // Check if the site requires all the selected blood types
        return requiredBloodTypes.containsAll(selectedBloodTypes);
    }
}