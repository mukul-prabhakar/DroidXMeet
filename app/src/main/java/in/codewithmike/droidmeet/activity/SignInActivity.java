package in.codewithmike.droidmeet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import in.codewithmike.droidmeet.R;
import in.codewithmike.droidmeet.databinding.ActivitySignInBinding;
import in.codewithmike.droidmeet.utilities.Constants;
import in.codewithmike.droidmeet.utilities.PreferenceManager;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private int[] images = {R.drawable.icon_show_password, R.drawable.icon_hide_password};
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        preferenceManager = new PreferenceManager(getApplicationContext());

        // if already signed in
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        // set default
        binding.passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.passwordVisibilityToggle.setImageResource(images[1]);

        binding.passwordVisibilityToggle.setOnClickListener(viewToggle -> {
            applyVisibilityMethod(); // visibility toggle button
        });

        binding.loginButton.setOnClickListener(viewLogin -> {
            if (TextUtils.isEmpty(binding.emailInput.getText())) {
                Toast.makeText(this, "Enter email address", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(String.valueOf(binding.emailInput.getText()).trim()).matches()) {
                Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(binding.passwordInput.getText())) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            } else {
                getUserData(
                        String.valueOf(binding.emailInput.getText()),
                        String.valueOf(binding.passwordInput.getText()));
            }
        });

        binding.signUpTextButton.setOnClickListener(
                viewSignUp -> startActivity(new Intent(this, SignUpActivity.class)));

    }

    /*** Method to switch password visibility toggle**/
    private void applyVisibilityMethod() {
        if (binding.passwordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            binding.passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.passwordVisibilityToggle.setImageResource(images[0]);
        } else if (binding.passwordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
            binding.passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.passwordVisibilityToggle.setImageResource(images[1]);
        }
    }

    /*** Method to get user data for logged in user**/
    private void getUserData(String userEmail, String userPassword) {

        // start checking for data
        binding.loginButton.setVisibility(View.INVISIBLE);
        binding.signInProgressbar.setVisibility(View.VISIBLE);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, userEmail)
                .whereEqualTo(Constants.KEY_PASSWORD, userPassword)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));

                        Toast.makeText(SignInActivity.this, "Sign-in successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        binding.loginButton.setVisibility(View.VISIBLE);
                        binding.signInProgressbar.setVisibility(View.INVISIBLE);
                        Toast.makeText(SignInActivity.this, "Unable to Sign-in", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // if unsuccessful
                    binding.loginButton.setVisibility(View.VISIBLE);
                    binding.signInProgressbar.setVisibility(View.INVISIBLE);
                    Toast.makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}