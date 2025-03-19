package rmit.ad.blooddonationsystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.AuthHelper;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;
import rmit.ad.blooddonationsystem.models.BloodType;

public class SignInActivity extends AppCompatActivity {

    AuthHelper authHelper = AuthHelper.getInstance();
    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();

    TextView invalidInputText;
    EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        invalidInputText = findViewById(R.id.wrongInformationTextView);
        emailEditText = findViewById(R.id.editEmailSignIn);
        passwordEditText = findViewById(R.id.editPasswordSignIn);

    }

    public void onBtnSignIn(View view) {
        // perform sign in
        String email, password;
        email = emailEditText.getText().toString();
        password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            invalidInputText.setText("Please fill out all the fields");
            findViewById(R.id.wrongInformationTextView).setVisibility(View.VISIBLE);
        } else {

            authHelper.signInWithEmailAndPassword(email, password, task -> {
                if (task.isSuccessful()) {

                    String userID = authHelper.getCurrentUser().getUid();
                    firestoreHelper.getUserData(userID, documentSnapshot -> {
                        if (documentSnapshot.exists()) {

                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            String userType = documentSnapshot.getString("userType");

                            Intent intent;
                            if (firstName == null && lastName == null) {
                                Toast.makeText(SignInActivity.this, "Welcome super user", Toast.LENGTH_SHORT).show();
                                intent = new Intent(SignInActivity.this, SuperUserActivity.class); // default activity
                            } else if ("donor".equals(userType)) {
                                Toast.makeText(SignInActivity.this, "Welcome, " + firstName + " " + lastName, Toast.LENGTH_SHORT).show();
                                intent = new Intent(SignInActivity.this, DonorActivity.class);
                            } else if ("site_manager".equals(userType)) {
                                Toast.makeText(SignInActivity.this, "Welcome, " + firstName + " " + lastName, Toast.LENGTH_SHORT).show();
                                intent = new Intent(SignInActivity.this, SiteManagerActivity.class);
                            } else {
                                intent = new Intent(SignInActivity.this, SignInActivity.class); // default activity
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("Firestore", "Document does not exist");
                            Toast.makeText(SignInActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                        }
                    }, e -> Log.d("Firestore", "Error retrieving data: " + task.getException()));
                } else {
                    Toast.makeText(SignInActivity.this, "Unsuccessful Sign In", Toast.LENGTH_SHORT).show();
                    invalidInputText.setText("Invalid email address or password");
                    findViewById(R.id.wrongInformationTextView).setVisibility(View.VISIBLE);
                }
            });

        }
    }

    public void onReturn(View view) {
        finish();
    }

    public void onForgotPassword(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter your registered email address to reset your password:");

        final EditText emailInput = new EditText(this);
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int marginInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        emailInput.setLayoutParams(params);
        container.addView(emailInput);

        builder.setView(container);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = emailInput.getText().toString();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(SignInActivity.this, "Email field cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                sendPasswordResetEmail(email);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void sendPasswordResetEmail(String email) {

        authHelper.sendPasswordResetEmail(email, task -> {
            if (task.isSuccessful()) {
                findViewById(R.id.resetMessageTextView).setVisibility(View.VISIBLE);
                findViewById(R.id.resetMessageTextView2).setVisibility(View.VISIBLE);
                findViewById(R.id.emailPasswordSignIn).setVisibility(View.INVISIBLE);
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                Toast.makeText(SignInActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}