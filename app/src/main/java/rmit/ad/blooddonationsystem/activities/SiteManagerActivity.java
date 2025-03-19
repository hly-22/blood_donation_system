package rmit.ad.blooddonationsystem.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.AuthHelper;
import rmit.ad.blooddonationsystem.helpers.DonorVolunteerAdapter;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;
import rmit.ad.blooddonationsystem.models.BloodType;
import rmit.ad.blooddonationsystem.models.Donation;
import rmit.ad.blooddonationsystem.models.Donor;
import rmit.ad.blooddonationsystem.models.Site;
import rmit.ad.blooddonationsystem.models.Volunteer;

public class SiteManagerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private String managerID;
    private Marker tempMarker;
    private LatLng selectedLocation;
    private Site currentSite;

    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();
    AuthHelper authHelper = AuthHelper.getInstance();

    LinearLayout newSite, addSiteForm, viewSite, editSiteForm, viewDonorVolunteerList, volunteerForm;
    TextView siteName, siteAddress, siteOpeningHours, requiredBloodTypes;
    ChipGroup chipGroupBloodTypes, editChipGroupBloodTypes;
    Button btnChangeInfo, btnConfirmEdit, btnViewDonors, btnViewVolunteers, btnAddVolunteer;

    private ListView dVListView;
    private DonorVolunteerAdapter dVAdapter;
    private DonorVolunteerAdapter VAdapter;
    private List<Object> donors = new ArrayList<>();
    private List<Object> volunteers = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_manager);

        managerID = authHelper.getCurrentUser().getUid();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.siteManagerMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        newSite = findViewById(R.id.newSite);
        addSiteForm = findViewById(R.id.addSiteForm);
        viewSite = findViewById(R.id.viewSite);
        editSiteForm = findViewById(R.id.editSiteForm);
        viewDonorVolunteerList = findViewById(R.id.viewDonorVolunteerList);
        volunteerForm = findViewById(R.id.volunteerForm);

        chipGroupBloodTypes = findViewById(R.id.chipGroupBloodTypes);
        editChipGroupBloodTypes = findViewById(R.id.editChipGroupBloodTypes);
    }

    private void getCurrentLocation() {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Move camera to current location (no marker)
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation, 15);
                            googleMap.moveCamera(cameraUpdate);
                        }
                    });
        } else {
            // Handle the case where permission is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        getCurrentLocation();

        loadSites();

        googleMap.setOnMapClickListener(latLng -> {
            // If there is already a temporary marker, remove it
            if (tempMarker != null) {
                tempMarker.remove();
            }

            // Add a new blue marker at the clicked location
            tempMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            // Update the selected location
            selectedLocation = latLng;

            // Show the bottom sheet
            newSite.setVisibility(View.VISIBLE);
            viewSite.setVisibility(View.INVISIBLE);
        });
    }
    public void loadSites() {
        firestoreHelper.getSitesByManager(managerID, new FirestoreHelper.FirestoreCallback<List<Site>>() {
            @Override
            public void onSuccess(List<Site> sites) {
                if (sites != null && !sites.isEmpty()) {
                    googleMap.clear();
                    // Loop through sites and add markers on the map
                    for (Site site : sites) {
                        if (site.isActive()) {
                            LatLng siteLocation = new LatLng(site.getLatitude(), site.getLongitude());

                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(siteLocation)
                                    .title(site.getLocationName());

                            Marker marker = googleMap.addMarker(markerOptions);
                            marker.setTag(site);
                        }
                    }

                    // Set a click listener on the markers
                    googleMap.setOnMarkerClickListener(marker -> {
                        Site selectedSite = (Site) marker.getTag();
                        if (selectedSite != null) {
                            showSiteInfo(selectedSite);
                        }
                        return false;
                    });
                } else {
                    // Handle case where no sites were found
                    Toast.makeText(SiteManagerActivity.this, "No sites found for this manager.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SiteManagerActivity.this, "Failed to load sites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showSiteInfo(Site site) {
        siteName = findViewById(R.id.siteNameView);
        siteName.setText(site.getLocationName());

        siteOpeningHours = findViewById(R.id.openingHoursView);
        siteOpeningHours.setText(site.getOpeningHours() != null ? site.getOpeningHours() : "Not available");

        requiredBloodTypes = findViewById(R.id.requiredBTView);
        site.getRequiredBloodTypes();
        requiredBloodTypes.setText(site.convertBloodTypesToString());

        siteAddress = findViewById(R.id.addressView);
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
                this.currentSite = site;
            } else {
                // Handle case where no address is found
                siteAddress.setText("Address not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors, e.g., network issues
            siteAddress.setText("Failed to get address.");
        }

        viewSite.setVisibility(View.VISIBLE);
        newSite.setVisibility(View.INVISIBLE);
        if (tempMarker != null && tempMarker.isVisible()) {
            tempMarker.setVisible(false);
        }

        btnChangeInfo = findViewById(R.id.btnChangeInfo);
        btnChangeInfo.setOnClickListener(v -> {
            viewSite.setVisibility(View.INVISIBLE);
            editSiteForm.setVisibility(View.VISIBLE);
            goToEditForm(site);
        });

        btnAddVolunteer = findViewById(R.id.btnAddVolunteer);

        btnViewDonors = findViewById(R.id.btnViewDonors);
        btnViewDonors.setOnClickListener(v -> {
            dVListView = findViewById(R.id.listItems);
            dVListView.setOnItemClickListener((parent, view, position, id) -> {
                Object selectedItem = donors.get(position); // Get the selected item
                String itemName;

                // Check if the item is a Donor or Volunteer
                if (selectedItem instanceof Donation) {
                    Donation donation = (Donation) selectedItem;
                    itemName = donation.getDonorFullName(); // Name to show in the dialog

                    // Show a dialog to input donation details for the donor
                    showDonationForm(donation); // Your existing function to show the form
                }
            });

            goToDonorsList();
            viewSite.setVisibility(View.INVISIBLE);
            viewDonorVolunteerList.setVisibility(View.VISIBLE);
            if (btnAddVolunteer.getVisibility() == View.VISIBLE) {
                btnAddVolunteer.setVisibility(View.GONE);
            }
        });


        btnViewVolunteers = findViewById(R.id.btnViewVolunteers);
        btnViewVolunteers.setOnClickListener(v -> {
            if (btnAddVolunteer.getVisibility() == View.GONE || btnAddVolunteer.getVisibility() == View.INVISIBLE) {
                btnAddVolunteer.setVisibility(View.VISIBLE);
            }
            dVListView = findViewById(R.id.listItems);
            dVListView.setOnItemClickListener((parent, view, position, id) -> {
                Object selectedItem = volunteers.get(position); // Get the selected item
                String itemName;

                if (selectedItem instanceof Volunteer) {
                    Volunteer volunteer = (Volunteer) selectedItem;
                    itemName = volunteer.getFullName(); // Name to show in the dialog

                    // Show a confirmation dialog
                    new AlertDialog.Builder(SiteManagerActivity.this)
                            .setTitle("Confirm Deletion")
                            .setMessage("Are you sure you want to delete " + itemName + "?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Delete from Firestore
                                firestoreHelper.deleteVolunteer(volunteer.getEmail(), new FirestoreHelper.FirestoreCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        // Remove from the list
                                        volunteers.remove(position);
                                        VAdapter.notifyDataSetChanged(); // Refresh the list
                                        Toast.makeText(SiteManagerActivity.this, "Volunteer deleted.", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(SiteManagerActivity.this, "Failed to delete volunteer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("No", null) // Close dialog without action
                            .show();
                }
            });
            goToVolunteerList();
            viewSite.setVisibility(View.INVISIBLE);
            viewDonorVolunteerList.setVisibility(View.VISIBLE);
        });
    }

    private void goToVolunteerList() {
        TextView title = findViewById(R.id.textView);
        title.setText("List of Volunteers");


        fetchVolunteersForSite(currentSite.getId());
    }

    private void fetchVolunteersForSite(String siteId) {
        firestoreHelper.getVolunteersBySite(siteId, new FirestoreHelper.FirestoreCallback<List<Volunteer>>() {
            @Override
            public void onSuccess(List<Volunteer> result) {
                if (result != null) {
                    volunteers.clear();
                    volunteers.addAll(result);

                    VAdapter = new DonorVolunteerAdapter(SiteManagerActivity.this, volunteers, false);
                    dVListView.setAdapter(VAdapter);
                    if (VAdapter != null) {
                        VAdapter.notifyDataSetChanged(); // Refresh the list view
                    }
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SiteManagerActivity.this, "Failed to load volunteers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToDonorsList() {
        TextView title = findViewById(R.id.textView);
        title.setText("List of Donors");

        fetchDonorsForSite(currentSite.getId());
    }

    private void fetchDonorsForSite(String siteId) {
        firestoreHelper.getDonorsBySite(siteId, new FirestoreHelper.FirestoreCallback<List<Donation>>() {
            @Override
            public void onSuccess(List<Donation> result) {
                if (result != null) {
                    donors.clear();
                    donors.addAll(result);

                    dVAdapter = new DonorVolunteerAdapter(SiteManagerActivity.this, donors, true);
                    dVListView.setAdapter(dVAdapter);

                    if (dVAdapter != null) {
                        dVAdapter.notifyDataSetChanged(); // Refresh the list view
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SiteManagerActivity.this, "Failed to load donors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDonationForm(Donation donation) {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View formView = inflater.inflate(R.layout.dialog_donation_form, null);

        // Get references to the views in the layout
        TextView tvDonorName = formView.findViewById(R.id.tvDonorName);
        TextView etDonationDate = formView.findViewById(R.id.etDonationDate);
        RadioButton rbSmallBag = formView.findViewById(R.id.rbSmallBag);
        RadioButton rbLargeBag = formView.findViewById(R.id.rbLargeBag);

        // Pre-fill donor name and data if it exists
        tvDonorName.setText(donation.getDonorFullName());

        if (donation.isHasDonate()) {
            etDonationDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(donation.getDonationDate()));

            if (donation.getBloodBagSize() == Donation.BloodBagSize.SMALL) {
                rbSmallBag.setChecked(true);
            } else {
                rbLargeBag.setChecked(true);
            }

            // Disable editing if already donated
            etDonationDate.setClickable(false);
            rbSmallBag.setEnabled(false);
            rbLargeBag.setEnabled(false);
        } else {
            etDonationDate.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                new DatePickerDialog(
                        SiteManagerActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                            etDonationDate.setText(selectedDate);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show();
            });
        }

        // Build the dialog
        new AlertDialog.Builder(this)
                .setTitle(donation.isHasDonate() ? "View Donation Details" : "Enter Donation Details")
                .setView(formView)
                .setPositiveButton(donation.isHasDonate() ? "OK" : "Save", (dialog, which) -> {
                    if (!donation.isHasDonate()) {
                        // Parse the date
                        String donationDateStr = etDonationDate.getText().toString();
                        Date donationDate = null;
                        try {
                            donationDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(donationDateStr);
                        } catch (ParseException e) {
                            Toast.makeText(this, "Invalid date format!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Determine the selected BloodBagSize
                        Donation.BloodBagSize selectedSize;
                        if (rbSmallBag.isChecked()) {
                            selectedSize = Donation.BloodBagSize.SMALL;
                        } else if (rbLargeBag.isChecked()) {
                            selectedSize = Donation.BloodBagSize.LARGE;
                        } else {
                            Toast.makeText(this, "Please select a blood bag size!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Save data to Firestore and update the local list
                        donation.setDonationDate(donationDate);
                        donation.setBloodBagSize(selectedSize);
                        donation.setHasDonate(true);

                        firestoreHelper.updateDonation(donation, new FirestoreHelper.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(SiteManagerActivity.this, "Donation details saved.", Toast.LENGTH_SHORT).show();
                                updateDonorList();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(SiteManagerActivity.this, "Failed to save donation details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDonorList() {
        dVAdapter.notifyDataSetChanged();
    }

    public void onBtnAddVolunteer(View view) {
        volunteerForm.setVisibility(View.VISIBLE);
    }

    public void onBtnConfirmVolunteer(View view) {
        EditText firstNameText = findViewById(R.id.volunteerFNameEdit);
        EditText lastNameText = findViewById(R.id.volunteerLNameEdit);
        EditText emailText = findViewById(R.id.volunteerEmailEdit);

        String firstName = firstNameText.getText().toString().trim();
        String lastName = lastNameText.getText().toString().trim();
        String email = emailText.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Volunteer nVolunteer = new Volunteer(email, null, firstName, lastName, currentSite.getId());

        firestoreHelper.addVolunteer(nVolunteer, new FirestoreHelper.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(SiteManagerActivity.this, "Volunteer added successfully.", Toast.LENGTH_SHORT).show();

                volunteers.add(nVolunteer);

                // Clear the form
                firstNameText.setText("");
                lastNameText.setText("");
                emailText.setText("");

                // Hide the form and reload the list
                volunteerForm.setVisibility(View.GONE);
                viewDonorVolunteerList.setVisibility(View.VISIBLE);

                // Reload the volunteer list from Firestore
                fetchVolunteersForSite(currentSite.getId());
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SiteManagerActivity.this, "Failed to add volunteer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBtnCancel(View view) {
        ((EditText) findViewById(R.id.volunteerFNameEdit)).setText("");
        ((EditText) findViewById(R.id.volunteerLNameEdit)).setText("");
        ((EditText) findViewById(R.id.volunteerEmailEdit)).setText("");
        volunteerForm.setVisibility(View.INVISIBLE);
        viewDonorVolunteerList.setVisibility(View.VISIBLE);
    }


    private void goToEditForm(Site site) {

        EditText locationNameEditText = findViewById(R.id.editLocationNameEditText);
        locationNameEditText.setText(site.getLocationName());

        EditText openingHoursEditText = findViewById(R.id.editOpeningHoursEditText);
        openingHoursEditText.setText(site.getOpeningHours());

        for (int i = 0; i < editChipGroupBloodTypes.getChildCount(); i++) {
            Chip chip = (Chip) editChipGroupBloodTypes.getChildAt(i);
            chip.setChecked(false); // Deselect all chips first
        }
        if (site.getRequiredBloodTypes() != null) {
            for (BloodType bloodType : site.getRequiredBloodTypes()) {
                for (int i = 0; i < editChipGroupBloodTypes.getChildCount(); i++) {
                    Chip chip = (Chip) editChipGroupBloodTypes.getChildAt(i);
                    if (chip.getText().toString().equals(bloodType.toString())) {
                        chip.setChecked(true); // Select the chips that are checked
                    }
                }
            }
        }

        Switch switchIsActive = findViewById(R.id.switchIsActive);
        switchIsActive.setChecked(site.isActive());

        btnConfirmEdit = findViewById(R.id.btnConfirmEdit);
        btnConfirmEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isActive = switchIsActive.isChecked();
                String locationName = locationNameEditText.getText().toString().trim();
                String openingHours = openingHoursEditText.getText().toString().trim();

                // Initialize updated requiredBloodTypes list
                List<BloodType> updatedBloodTypes = new ArrayList<>();
                for (int i = 0; i < editChipGroupBloodTypes.getChildCount(); i++) {
                    Chip chip = (Chip) editChipGroupBloodTypes.getChildAt(i);
                    if (chip.isChecked()) {
                        updatedBloodTypes.add(BloodType.getBloodTypeFromText(chip.getText().toString()));
                    }
                }

                // If any field is empty, don't apply changes
                if (locationName.isEmpty() || openingHours.isEmpty()) {
                    Toast.makeText(SiteManagerActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }


                // Create the updated Site object
                Site updatedSite = new Site(site.getAssociatedSiteManager(), locationName, site.getLatitude(), site.getLongitude(), openingHours, updatedBloodTypes, isActive);
                updatedSite.setId(site.getId());  // Ensure we keep the original site ID for updating

                // Update site in Firestore
                firestoreHelper.updateSiteData(updatedSite, new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // After successfully updating, reload the sites to show the updated info
                        loadSites();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SiteManagerActivity.this, "Error updating site: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                // Clear the form fields after updating
                locationNameEditText.setText("");
                openingHoursEditText.setText("");
                chipGroupBloodTypes.clearCheck();

                editSiteForm.setVisibility(View.INVISIBLE);

                Toast.makeText(SiteManagerActivity.this, "Site updated successfully.", Toast.LENGTH_SHORT).show();
                loadSites();
            }
        });
    }

    public void onBtnSignOut(View view) {
        authHelper.signOut(SiteManagerActivity.this);
    }

    public void onBtnConfirmNewSite(View view) {
        newSite.setVisibility(View.INVISIBLE);
        viewSite.setVisibility(View.INVISIBLE);
        addSiteForm.setVisibility(View.VISIBLE);
    }

    public void onBtnConfirm(View view) {
        // Get form inputs
        EditText locationNameEditText = findViewById(R.id.locationNameEditText);
        String locationName = locationNameEditText.getText().toString().trim();

        EditText openingHoursEditText = findViewById(R.id.openingHoursEditText);
        String openingHours = openingHoursEditText.getText().toString().trim();

        // Initialize requiredBloodTypes list
        List<BloodType> requiredBloodTypes = new ArrayList<>();
        for (int i = 0; i < chipGroupBloodTypes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupBloodTypes.getChildAt(i);
            if (chip.isChecked()) {
                Log.d("ChipClicked", "Chip clicked: " + chip.getText());

                requiredBloodTypes.add(BloodType.getBloodTypeFromText(chip.getText().toString())); // Convert text to BloodType
            }
        }

        // Validate inputs
        if (locationName.isEmpty() || openingHours.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Site nSite = new Site(managerID, locationName, selectedLocation.latitude, selectedLocation.longitude, openingHours, requiredBloodTypes, true);

        firestoreHelper.addSiteDatas(nSite);
        locationNameEditText.setText("");
        openingHoursEditText.setText("");
        chipGroupBloodTypes.clearCheck();
        addSiteForm.setVisibility(View.GONE);

        Toast.makeText(this, "New site data collected.", Toast.LENGTH_SHORT).show();
        loadSites();
    }

    public void onBtnClose(View view) {
        if (addSiteForm.getVisibility() == View.VISIBLE) {
            addSiteForm.setVisibility(View.INVISIBLE);
        }
        if (editSiteForm.getVisibility() == View.VISIBLE) {
            editSiteForm.setVisibility(View.INVISIBLE);
        }
        if (viewDonorVolunteerList.getVisibility() == View.VISIBLE) {
            viewDonorVolunteerList.setVisibility(View.INVISIBLE);
        }
        // for other Close buttons
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                // Handle the case where the permission is denied
            }
        }
    }
}