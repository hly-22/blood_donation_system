package rmit.ad.blooddonationsystem.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import rmit.ad.blooddonationsystem.R;
import rmit.ad.blooddonationsystem.activities.MainActivity;
import rmit.ad.blooddonationsystem.activities.SignInActivity;
import rmit.ad.blooddonationsystem.activities.SignUpActivity;

public class AuthHelper {

    private static AuthHelper instance;
    private FirebaseAuth auth;

    private AuthHelper() {
        auth = FirebaseAuth.getInstance();
    }

    public static AuthHelper getInstance() {
        if (instance == null) {
            instance = new AuthHelper();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }

    public void signOut(Context context) {
        signOut();
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void signInWithCredential(AuthCredential credential, OnCompleteListener<AuthResult> onCompleteListener) {
        auth.signInWithCredential(credential).addOnCompleteListener(onCompleteListener);
    }

    public void signInWithEmailAndPassword(String email, String password, OnCompleteListener<AuthResult> onCompleteListener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(onCompleteListener);
    }

    public void sendPasswordResetEmail(String email, OnCompleteListener<Void> onCompleteListener) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(onCompleteListener);
    }

    public void createUserWithEmailAndPassword(String email, String password, OnCompleteListener<AuthResult> onCompleteListener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(onCompleteListener);
    }

}
