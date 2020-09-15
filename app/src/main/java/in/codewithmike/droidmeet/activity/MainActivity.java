package in.codewithmike.droidmeet.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.codewithmike.droidmeet.adapters.UsersAdapter;
import in.codewithmike.droidmeet.databinding.ActivityMainBinding;
import in.codewithmike.droidmeet.listeners.UsersListener;
import in.codewithmike.droidmeet.models.User;
import in.codewithmike.droidmeet.utilities.Constants;
import in.codewithmike.droidmeet.utilities.PreferenceManager;

public class MainActivity extends AppCompatActivity implements UsersListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<User> users;
    private UsersAdapter usersAdapter;

    private int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        preferenceManager = new PreferenceManager(getApplicationContext());

        binding.logoutTextButton.setOnClickListener(view1 -> {
            logoutFromApp(); /* logout from app*/
        });

        // Get Firebase token
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });

        users = new ArrayList<>();
        usersAdapter = new UsersAdapter(users, this);
        binding.userRecyclerView.setAdapter(usersAdapter);

        // swipe refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        getUsers(); // get all users from database
        checkForBatteryOptimizations();

    }

    /*** Method to fetch user data**/
    private void getUsers() {
        binding.swipeRefreshLayout.setRefreshing(true);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    binding.swipeRefreshLayout.setRefreshing(false);

                    String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        users.clear(); // clear list before overriding new data
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (myUserId.equals(documentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.userEmail = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.userToken = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            users.add(user);
                            if (users.size() > 0) {
                                usersAdapter.notifyDataSetChanged();
                            } else {
                                binding.textErrorMessage.setText(String.format("%s", "No users available"));
                                binding.textErrorMessage.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        binding.textErrorMessage.setText(String.format("%s", "No users available"));
                        binding.textErrorMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    /*** Method to fcm token to user data**/
    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /*** Method to clear fcm token from user data and logout**/
    private void logoutFromApp() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());

        documentReference.update(updates)
                .addOnSuccessListener(clearReference -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                    Toast.makeText(this, "Logged Out...", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to logout", Toast.LENGTH_SHORT).show());
    }

    /*** Initiate Video Meeting**/
    @Override
    public void initiateVideoMeeting(User user) {
        if (user.userToken == null || user.userToken.trim().isEmpty()) {
            Toast.makeText(this,
                    user.firstName + " " + user.lastName + "not available for video meeting",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Intent initiateVideoIntent = new Intent(getApplicationContext(), OutgoingMeetingInvitation.class);
            initiateVideoIntent.putExtra("user", user);
            initiateVideoIntent.putExtra("type", "video");
            startActivity(initiateVideoIntent);
        }
    }

    /*** Initiate Audio Meeting**/
    @Override
    public void initiateAudioMeeting(User user) {
        if (user.userToken == null || user.userToken.trim().isEmpty()) {
            Toast.makeText(this,
                    user.firstName + " " + user.lastName + "not available for audio meeting",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Intent initiateAudioIntent = new Intent(getApplicationContext(), OutgoingMeetingInvitation.class);
            initiateAudioIntent.putExtra("user", user);
            initiateAudioIntent.putExtra("type", "audio");
            startActivity(initiateAudioIntent);
        }
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            binding.imageConference.setVisibility(View.VISIBLE);
            binding.imageConference.setOnClickListener(view -> {
                Intent conferenceIntent = new Intent(getApplicationContext(), OutgoingMeetingInvitation.class);
                conferenceIntent.putExtra("selectedUsers", new Gson().toJson(usersAdapter.getSelectedUsers()));
                conferenceIntent.putExtra("type", "video");
                conferenceIntent.putExtra("isMultiple", true);
                startActivity(conferenceIntent);
            });
        } else {
            binding.imageConference.setVisibility(View.GONE);
        }
    }

    private void checkForBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enabled. It can interrupt running background services");
                builder.setPositiveButton("Disable", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                });
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATIONS) {
            checkForBatteryOptimizations();
        }
    }
}