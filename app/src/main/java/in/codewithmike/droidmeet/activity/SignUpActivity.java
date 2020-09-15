package in.codewithmike.droidmeet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import in.codewithmike.droidmeet.databinding.ActivitySignUpBinding;
import in.codewithmike.droidmeet.utilities.Constants;
import in.codewithmike.droidmeet.utilities.PreferenceManager;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        preferenceManager = new PreferenceManager(getApplicationContext());

        binding.navigationBack.setOnClickListener(
                viewNavigateBack -> onBackPressed());

        binding.registerButton.setOnClickListener(viewRegister -> {
            if (TextUtils.isEmpty(binding.firstNameInput.getText())) {
                Toast.makeText(this, "Enter first name", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(binding.lastNameInput.getText())) {
                Toast.makeText(this, "Enter last name", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(binding.emailInput.getText())) {
                Toast.makeText(this, "Enter email address", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(String.valueOf(binding.emailInput.getText()).trim()).matches()) {
                Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(binding.passwordInput.getText())) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(binding.confirmPasswordInput.getText())) {
                Toast.makeText(this, "confirm your password", Toast.LENGTH_SHORT).show();
            } else if (!(String.valueOf(binding.passwordInput.getText()).trim())
                    .equals(String.valueOf(binding.confirmPasswordInput.getText()).trim())) {
                Toast.makeText(this, "Password & confirm password must be same", Toast.LENGTH_SHORT).show();
            } else {
                insertNewData(
                        String.valueOf(binding.firstNameInput.getText()),
                        String.valueOf(binding.lastNameInput.getText()),
                        String.valueOf(binding.emailInput.getText()),
                        String.valueOf(binding.confirmPasswordInput.getText()));
            }
        });
    }

    /*** Method to create new user data**/
    private void insertNewData(String firstName, String lastName, String userEmail, String userPassword) {

        // start inserting data
        binding.registerButton.setVisibility(View.INVISIBLE);
        binding.signUpProgressbar.setVisibility(View.VISIBLE);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, firstName);
        user.put(Constants.KEY_LAST_NAME, lastName);
        user.put(Constants.KEY_EMAIL, userEmail);
        user.put(Constants.KEY_PASSWORD, userPassword);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, firstName);
                    preferenceManager.putString(Constants.KEY_LAST_NAME, lastName);
                    preferenceManager.putString(Constants.KEY_EMAIL, userEmail);

                    Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    // if unsuccessful
                    binding.registerButton.setVisibility(View.VISIBLE);
                    binding.signUpProgressbar.setVisibility(View.INVISIBLE);
                    Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}