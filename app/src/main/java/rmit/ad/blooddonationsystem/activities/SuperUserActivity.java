package rmit.ad.blooddonationsystem.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;
import rmit.ad.blooddonationsystem.models.Donation;
import rmit.ad.blooddonationsystem.models.Site;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class SuperUserActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();
    LinearLayout reportSection;
    Site selectedSite;
    List<Donation> donationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_user);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        reportSection = findViewById(R.id.reportSection);
        reportSection.setVisibility(View.GONE);

        Button btnSaveToPdf = findViewById(R.id.btnSaveToPdf);

    }

    public void onBtnSaveToPdf(View view) {
        generatePDFReport(donationsList, selectedSite);
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(marker -> {
            Site mSite = (Site) marker.getTag();
            selectedSite = mSite;
            if (mSite != null) {
                // Load report section for the selected site
                loadReportForSite(mSite);
            }
            return false;
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        loadDonationSites();
    }

    private void loadReportForSite(Site site) {
        // Fetch donation data for the selected site from Firestore
        firestoreHelper.getDonationsBySite(site.getId(), new FirestoreHelper.FirestoreCallback<List<Donation>>() {
            @Override
            public void onSuccess(List<Donation> donations) {
                donationsList = donations;
                // Clear previous data in the report section
                TableLayout tblReport = findViewById(R.id.tblReport);
                tblReport.removeAllViews();

                // Add headers to the table (if not already present)
                if (tblReport.getChildCount() == 0) {
                    TableRow headerRow = new TableRow(SuperUserActivity.this);
                    headerRow.setBackgroundColor(getResources().getColor(R.color.blue_variant));

                    // Create header cells
                    String[] headers = {"Donation Date", "Donor", "Blood Type", "Bag Size"};
                    for (String header : headers) {
                        TextView headerCell = new TextView(SuperUserActivity.this);
                        headerCell.setText(header);
                        headerCell.setTextColor(Color.WHITE);
                        headerCell.setGravity(Gravity.CENTER);
                        headerCell.setPadding(8, 8, 8, 8);
                        headerRow.addView(headerCell);
                    }
                    tblReport.addView(headerRow);
                }

                // Populate the table with the donation data
                for (Donation donation : donations) {
                    TableRow row = new TableRow(SuperUserActivity.this);

                    // Create table cells for each donation
                    TextView idCell = new TextView(SuperUserActivity.this);
                    idCell.setText(formatDate(donation.getDonationDate()));
                    idCell.setGravity(Gravity.CENTER);
                    row.addView(idCell);

                    TextView donorNameCell = new TextView(SuperUserActivity.this);
                    donorNameCell.setText(donation.getDonorFullName());
                    donorNameCell.setGravity(Gravity.CENTER);
                    row.addView(donorNameCell);

                    TextView bloodTypeCell = new TextView(SuperUserActivity.this);
                    bloodTypeCell.setText(donation.getDonorBloodType().toString());
                    bloodTypeCell.setGravity(Gravity.CENTER);
                    row.addView(bloodTypeCell);

                    TextView bagSizeCell = new TextView(SuperUserActivity.this);
                    bagSizeCell.setText(donation.getBloodBagSizeVolume() + " mL");
                    bagSizeCell.setGravity(Gravity.CENTER);
                    row.addView(bagSizeCell);

                    // Add the row to the table
                    tblReport.addView(row);
                }

                // Optionally, update summary information (e.g., total donors, total blood donated)
                updateSummary(donations);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SuperUserActivity.this, "Failed to load report data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary(List<Donation> donations) {
        int totalDonors = donations.size();
        int totalBlood = donations.stream().mapToInt(Donation::getBloodBagSizeVolume).sum();

        TextView tvTotalDonors = findViewById(R.id.tvTotalDonors);
        tvTotalDonors.setText("Total Donors: " + totalDonors);

        TextView tvTotalBloodAmount = findViewById(R.id.tvTotalBloodAmount);
        tvTotalBloodAmount.setText("Total Blood: " + totalBlood + " mL");

        reportSection.setVisibility(View.VISIBLE);
    }


    private void loadDonationSites() {
        firestoreHelper.getAllSites(new FirestoreHelper.FirestoreCallback<List<Site>>() {
            @Override
            public void onSuccess(List<Site> sites) {
                for (Site site : sites) {
                    addMarker(site);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SuperUserActivity.this, "Failed to load sites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(Site site) {
        LatLng position = new LatLng(site.getLatitude(), site.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String siteAddress;
        try {
            List<Address> addresses = geocoder.getFromLocation(position.latitude, position.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String addressLine = address.getAddressLine(0);
                String city = address.getLocality();
                String country = address.getCountryName();

                String formattedAddress = (addressLine != null && !addressLine.isEmpty())
                        ? addressLine
                        : String.format("%s%s", city != null ? city : "", country != null ? ", " + country : "");
                siteAddress = formattedAddress;
            } else {
                // Handle case where no address is found
                siteAddress = "No address found.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any errors, e.g., network issues
            siteAddress = "Failed to get address.";
        }
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(site.getLocationName())
                .snippet("Address: " + siteAddress);

        // Customize marker color if needed
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        // Add marker to the map
        mMap.addMarker(markerOptions).setTag(site);
    }

    private void generatePDFReport(List<Donation> donations, Site site) {
        // Create a PdfDocument instance
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        int y = 50; // Starting y-coordinate for drawing

        // Draw the title
        paint.setTextSize(18);
        paint.setColor(Color.BLACK);
        canvas.drawText("Donation Report for " + site.getLocationName(), 50, y, paint);

        // Summary information
        y += 30;
        int totalDonors = donations.size();
        int totalBlood = 0;
        for (Donation donation : donations) {
            totalBlood += donation.getBloodBagSizeVolume();
        }
        paint.setTextSize(14);
        canvas.drawText("Total Donors: " + totalDonors, 50, y, paint);
        y += 20;
        canvas.drawText("Total Blood Donated: " + totalBlood + " mL", 50, y, paint);

        // Table Header
        y += 40;
        paint.setTextSize(12);
        paint.setColor(Color.WHITE);
        canvas.drawRect(50, y, 545, y + 30, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText("Donation Date", 60, y + 20, paint);
        canvas.drawText("Donor", 200, y + 20, paint);
        canvas.drawText("Blood Type", 350, y + 20, paint);
        canvas.drawText("Bag Size", 450, y + 20, paint);

        // Table Rows
        y += 40;
        for (Donation donation : donations) {
            if (y > 800) { // Create a new page if needed
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
            canvas.drawText(formatDate(donation.getDonationDate()), 60, y, paint);
            canvas.drawText(donation.getDonorFullName(), 200, y, paint);
            canvas.drawText(donation.getDonorBloodType().toString(), 350, y, paint);
            canvas.drawText(donation.getBloodBagSizeVolume() + " mL", 450, y, paint);
            y += 20;
        }

        // Finish the page
        pdfDocument.finishPage(page);

        // Save the PDF file
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Donation_Report_" + site.getLocationName() + ".pdf";
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            pdfDocument.writeTo(fos);
            fos.close();
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        pdfDocument.close();
    }

    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}