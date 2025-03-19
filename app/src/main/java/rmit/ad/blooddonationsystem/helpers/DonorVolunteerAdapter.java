package rmit.ad.blooddonationsystem.helpers;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.models.BloodType;
import rmit.ad.blooddonationsystem.models.Donation;
import rmit.ad.blooddonationsystem.models.Volunteer;

public class DonorVolunteerAdapter extends BaseAdapter {

    private Context context;
    private List<Object> items; // List containing either donors or volunteers
    private boolean isDonorList; // True if the list is for donors, false for volunteers

    public DonorVolunteerAdapter(Context context, List<Object> items, boolean isDonorList) {
        this.context = context;
        this.items = items;
        this.isDonorList = isDonorList;
    }

    @Override
    public int getCount() {
        return items.isEmpty() ? 1 : items.size(); // Return 1 for "No records" placeholder if empty
    }

    @Override
    public Object getItem(int position) {
        return items.isEmpty() ? null : items.get(position); // Return null if no records
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (items.isEmpty()) {
            // Inflate the placeholder view for "No records"
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.placeholder_no_records, parent, false);
            }
            TextView noRecordsText = convertView.findViewById(R.id.noRecordsText);
            noRecordsText.setText(isDonorList ? "No donors found." : "No volunteers found.");
        } else {
            // Inflate the regular view for donors or volunteers
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.activity_view_donor_volunteer_record, parent, false);
            }

            // Get references to the views in the layout
            TextView countID = convertView.findViewById(R.id.countID);
            TextView textName = convertView.findViewById(R.id.textName);
            TextView textSecond = convertView.findViewById(R.id.textSecond);
            TextView donatedDonor = convertView.findViewById(R.id.donatedDonor);

            Log.d("DonorVolunteerAdapter", "countID: " + (countID == null ? "null" : "not null"));
            Log.d("DonorVolunteerAdapter", "textName: " + (textName == null ? "null" : "not null"));
            Log.d("DonorVolunteerAdapter", "textSecond: " + (textSecond == null ? "null" : "not null"));
            Log.d("DonorVolunteerAdapter", "donatedDonor: " + (donatedDonor == null ? "null" : "not null"));

            // Set the item number (position + 1 for 1-based index)
            countID.setText(String.valueOf(position + 1));

            // Get the current item
            Object currentItem = items.get(position);

            // Populate the views based on the type of list (donors or volunteers)
            if (isDonorList) {
                // Cast the object to Donation
                Donation donation = (Donation) currentItem;

                textName.setText(donation.getDonorFullName());
                textSecond.setText(donation.getDonorBloodType().toString());
                textSecond.setVisibility(View.VISIBLE);

                // Show or hide the "Donated" text based on the donation's donation status
                donatedDonor.setVisibility(donation.isHasDonate() ? View.VISIBLE : View.GONE);

            } else {
                // Cast the object to Volunteer
                Volunteer volunteer = (Volunteer) currentItem;

                textName.setText(volunteer.getFullName());
                textSecond.setText(volunteer.getEmail());
                textSecond.setVisibility(View.VISIBLE);

                // Hide the "Donated" text for volunteers
                donatedDonor.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

}

