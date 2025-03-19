package rmit.ad.blooddonationsystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.helpers.AuthHelper;
import rmit.ad.blooddonationsystem.helpers.FirestoreHelper;

public class MainActivity extends AppCompatActivity {

    AuthHelper authHelper = AuthHelper.getInstance();
    FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();

    // Google sign in
    GoogleSignInClient googleSignInClient;
    ShapeableImageView shapeableImageView;
    TextView name, email;
    SignInButton signInButton;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

                    authHelper.signInWithCredential(authCredential, task -> {
                        if (task.isSuccessful()) {
                            Glide.with(MainActivity.this).load(Objects.requireNonNull(authHelper.getCurrentUser()).getPhotoUrl()).into(shapeableImageView);
                            name.setText(authHelper.getCurrentUser().getDisplayName());
                            email.setText(authHelper.getCurrentUser().getEmail());
                            Toast.makeText(MainActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                            findViewById(R.id.signInOptions).setVisibility(View.INVISIBLE);
                            findViewById(R.id.signOutGoogle).setVisibility(View.VISIBLE);
                            findViewById(R.id.goToActivity).setVisibility(View.VISIBLE);
                            findViewById(R.id.goToActivity).setOnClickListener(v -> goToUserActivity(authHelper.getCurrentUser().getUid()));
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authHelper.signOut();

        if (authHelper.getCurrentUser() != null) {
            String userID = authHelper.getCurrentUser().getUid();
            goToUserActivity(userID);
        } else {

            shapeableImageView = findViewById(R.id.profileImage);
            name = findViewById(R.id.profileName);
            email = findViewById(R.id.profileEmail);

            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);

            signInButton = findViewById(R.id.signInGoogle);
            signInButton.setOnClickListener(v -> googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }));
        }
    }

    private void goToUserActivity(String userID) {
        firestoreHelper.getUserData(userID, documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userType = documentSnapshot.getString("userType");

                // Redirect to the correct activity based on userType
                Intent intent;
                if ("donor".equals(userType)) {
                    intent = new Intent(MainActivity.this, DonorActivity.class);
                } else if ("site_manager".equals(userType)) {
                    intent = new Intent(MainActivity.this, SiteManagerActivity.class);
                } else if ("super_user".equals(userType)) {
                    intent = new Intent(MainActivity.this, SuperUserActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, MainActivity.class); // default activity
                }
                Log.d("MainActivity", userType + " is in the system");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close MainActivity
            } else {
                // Handle case where user document doesn't exist
                if (googleSignInClient != null) {
                    Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                    intent.putExtra("googleUserID", userID);
                    intent.putExtra("googleEmail", authHelper.getCurrentUser().getEmail());
                    startActivity(intent);
                } else {
                    Log.e("MainActivity", "User document does not exist.");
                }
            }
        }, e -> Log.e("MainActivity", "Error getting user type", e));
    }

    public void onSignInOption(View view) {
        startActivity(new Intent(MainActivity.this, SignInActivity.class));
    }

    public void onRegisterOption(View view) {
        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
    }

    public void onSignOutGoogle(View view) {
        authHelper.signOut(MainActivity.this);

    }
}