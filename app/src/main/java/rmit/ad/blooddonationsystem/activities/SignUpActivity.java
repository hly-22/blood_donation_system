package rmit.ad.blooddonationsystem.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.AuthHelper;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;
import rmit.ad.blooddonationsystem.models.BloodType;
import rmit.ad.blooddonationsystem.models.Donor;
import rmit.ad.blooddonationsystem.models.SiteManager;

public class SignUpActivity extends AppCompatActivity {

    AuthHelper authHelper = AuthHelper.getInstance();
    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();

    Button btnSignUpDonor, btnSignUpSiteManager;
    EditText editEmailSignUp, editPasswordSignUp, editVerifyPasswordSignUp;

    // Users attribute
    private String userID;
    private String userEmail;
    private String firstName;
    private String lastName;
    private Donor.Sex selectedSex;
    private Date dateOfBirth;
    private BloodType selectedBloodType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        editEmailSignUp = findViewById(R.id.editEmailSignUp);
        editPasswordSignUp = findViewById(R.id.editPasswordSignUp);
        editVerifyPasswordSignUp = findViewById(R.id.editVerifyPasswordSignUp);
        btnSignUpDonor = findViewById(R.id.btnSignUpDonor);
        btnSignUpSiteManager = findViewById(R.id.btnSignUpSiteManager);

        Intent intent = getIntent();

        if (intent.getExtras() != null) {
            userID = intent.getStringExtra("googleUserID");
            userEmail = intent.getStringExtra("googleEmail");
            editEmailSignUp.setText(userEmail);
            setDisableFields(editEmailSignUp);
            editPasswordSignUp.setVisibility(View.GONE);
            editVerifyPasswordSignUp.setVisibility(View.GONE);
        }
    }



    // Create Site Manager account
    public void onBtnSignUpSiteManager(View view) {
        String email, password, verifyPassword;
        email = editEmailSignUp.getText().toString();
        password = editPasswordSignUp.getText().toString();
        verifyPassword = editVerifyPasswordSignUp.getText().toString();

        if (editPasswordSignUp.getVisibility() == View.GONE) {
            handleSiteManagerSignUp();
            return;
        }

        if (validateFields(email, password, verifyPassword)) {

            authHelper.createUserWithEmailAndPassword(email, password, task -> {
                if (task.isSuccessful()) {

                    userID = authHelper.getCurrentUser().getUid();
                    userEmail = authHelper.getCurrentUser().getEmail();

                    handleSiteManagerSignUp();
                } else {
                    Toast.makeText(SignUpActivity.this, "Failed to sign up for Site Manager", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleSiteManagerSignUp() {
        findViewById(R.id.emailPasswordSignUp).setVisibility(View.GONE);
        findViewById(R.id.siteManagerSignUp).setVisibility(View.VISIBLE);
    }

    public void onBtnSiteManagerSignUp(View view) {
        // names
        EditText firstNameEditText = findViewById(R.id.firstNameEditText2);
        firstName = firstNameEditText.getText().toString();

        EditText lastNameEditText = findViewById(R.id.lastNameEditText2);
        lastName = lastNameEditText.getText().toString();

        // phone number
        EditText phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        String phoneNumber = phoneNumberEditText.getText().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() ) {
            Toast.makeText(SignUpActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
        } else {
            SiteManager siteManager = new SiteManager(userEmail, userID, firstName, lastName, phoneNumber);
            addSiteManagerToFirestore(siteManager.getId(), siteManager, "site_manager");

            Intent intent = new Intent(SignUpActivity.this, SiteManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    // Create Donor account
    public void onBtnSignUpDonor(View view) {
        String email, password, verifyPassword;
        email = editEmailSignUp.getText().toString();
        password = editPasswordSignUp.getText().toString();
        verifyPassword = editVerifyPasswordSignUp.getText().toString();

        if (editPasswordSignUp.getVisibility() == View.GONE) {
            handleDonorSignUp();
            return;
        }

        if (validateFields(email, password, verifyPassword)) {

            authHelper.createUserWithEmailAndPassword(email, password, task -> {
                if (task.isSuccessful()) {

                    userID = authHelper.getCurrentUser().getUid();
                    userEmail = authHelper.getCurrentUser().getEmail();

                    handleDonorSignUp();
                } else {
                    Toast.makeText(SignUpActivity.this, "Failed to sign up for Donor", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void handleDonorSignUp() {
        findViewById(R.id.emailPasswordSignUp).setVisibility(View.GONE);
        findViewById(R.id.proxyDonorForm).setVisibility(View.VISIBLE);

        // date of birth
        TextView dateOfBirthEditText = findViewById(R.id.dateOfBirthEditText);
        dateOfBirthEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, day1) -> {
                // Format selected date to YYYY-MM-DD
                String formattedMonth = String.format("%02d", month1 + 1); // +1 to account for 0-based month index
                String formattedDay = String.format("%02d", day1);
                String selectedDate = year1 + "-" + formattedMonth + "-" + formattedDay;

                dateOfBirthEditText.setText(selectedDate);

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
        Spinner bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner);

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

    public void onBtnDonorSignUp(View view) {
        // names
        EditText firstNameEditText = findViewById(R.id.firstNameEditText);
        firstName = firstNameEditText.getText().toString();

        EditText lastNameEditText = findViewById(R.id.lastNameEditText);
        lastName = lastNameEditText.getText().toString();

        // sex
        RadioGroup sexRadioGroup = findViewById(R.id.sexRadioGroup);
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
            Toast.makeText(SignUpActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
        } else {
            Donor donor = new Donor(userEmail, userID, firstName, lastName, selectedSex, dateOfBirth, selectedBloodType);
            addUserToFirestore(donor.getId(), donor, "donor");

            Intent intent = new Intent(SignUpActivity.this, DonorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    public void onReturn(View view) {
        finish();
    }

    private void addSiteManagerToFirestore(String id, SiteManager user, String userType) {
        // Convert the user object into a Map
        Map<String, Object> userMap = new Gson().fromJson(new Gson().toJson(user), Map.class);

        // Add the extra userType field
        userMap.put("userType", userType);

        // Add to Firestore
        firestoreHelper.addUserData(id, userType, userMap, SignUpActivity.this);
    }

    private <T> void addUserToFirestore(String id, T user, String userType) {
//        // Convert the user object into a Map
//        Map<String, Object> userMap = new Gson().fromJson(new Gson().toJson(user), Map.class);
//
//        // Add the extra userType field
//        userMap.put("userType", userType);

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
        firestoreHelper.addUserData(id, userType, userMap, SignUpActivity.this);
    }

    // Helper methods
    private boolean validateFields(String email, String password, String verifyPassword) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(verifyPassword)) {
            Toast.makeText(SignUpActivity.this, "No empty fields allowed", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must contain at least 8 characters, including numbers and letters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(verifyPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private boolean isValidPassword(String password) {
        // Password must be at least 8 characters long and contain at least one letter and one number
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        return password.matches(passwordPattern);
    }
    private void setDisableFields(EditText editText) {
        editText.setClickable(false);
        editText.setFocusable(false);
        editText.setCursorVisible(false);
    }
}