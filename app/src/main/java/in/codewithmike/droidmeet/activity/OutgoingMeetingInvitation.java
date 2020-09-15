package in.codewithmike.droidmeet.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import in.codewithmike.droidmeet.R;
import in.codewithmike.droidmeet.databinding.ActivityOutgoingMeetingInvitationBinding;
import in.codewithmike.droidmeet.models.User;
import in.codewithmike.droidmeet.network.ApiClient;
import in.codewithmike.droidmeet.network.ApiService;
import in.codewithmike.droidmeet.utilities.Constants;
import in.codewithmike.droidmeet.utilities.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingMeetingInvitation extends AppCompatActivity {

    private ActivityOutgoingMeetingInvitationBinding binding;
    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    private String meetingRoom = null;
    private String meetingType = null;

    private int rejectionCount = 0;
    private int totalReceivers = 0;
    /*** Method to listen response from receiver **/
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                    try {
                        URL serverURL = new URL("https://meet.jit.si");
                        JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                        builder.setServerURL(serverURL);
                        builder.setWelcomePageEnabled(false);
                        builder.setRoom(meetingRoom);
                        if (meetingType.equals("audio")) {
                            builder.setVideoMuted(true);
                        }
                        JitsiMeetActivity.launch(OutgoingMeetingInvitation.this, builder.build());
                        finish();
                    } catch (Exception exception) {
                        Toast.makeText(
                                OutgoingMeetingInvitation.this,
                                exception.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    }
                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    rejectionCount += 1;
                    if (rejectionCount == totalReceivers) {
                        Toast.makeText(context, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =
                ActivityOutgoingMeetingInvitationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        preferenceManager = new PreferenceManager(getApplicationContext());
        meetingType = getIntent().getStringExtra("type");

        String meetingType = getIntent().getStringExtra("type");
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                binding.imageMeetingType.setImageResource(R.drawable.icon_video_cam);
            } else if (meetingType.equals("audio")) {
                binding.imageMeetingType.setImageResource(R.drawable.icon_call);
            }
        }

        User user = (User) getIntent().getSerializableExtra("user");
        if (user != null) {
            binding.firstCharText.setText(user.firstName.substring(0, 1));
            binding.userNameTextView.setText(String.format("%s %s", user.firstName, user.lastName));
            binding.userEmailTextView.setText(user.userEmail);
        }

        binding.cancelInvitationButton.setOnClickListener(view1 -> {
            if (getIntent().getBooleanExtra("isMultiple", false)) {
                Type type = new TypeToken<ArrayList<User>>() {
                }.getType();
                ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                cancelInvitation(null, receivers);
            } else {
                if (user != null) {
                    cancelInvitation(user.userToken, null);
                }
            }
        });

        // Get Firebase token
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                inviterToken = task.getResult().getToken();
                if (meetingType != null) {
                    if (getIntent().getBooleanExtra("isMultiple", false)) {
                        Type type = new TypeToken<ArrayList<User>>() {
                        }.getType();
                        ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                        if (receivers != null) {
                            totalReceivers = receivers.size();
                        }
                        initiateMeeting(meetingType, null, receivers);
                    } else {
                        if (user != null) {
                            totalReceivers = 1;
                            initiateMeeting(meetingType, user.userToken, null); // initiate meeting
                        }
                    }
                }


            }
        });

    }

    /**
     * Method to Initiate Meeting
     *
     * @param meetingType   is the type of meeting invitation sent 'video/audio'
     * @param receiverToken is the receiver token
     **/
    private void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();

            if (receiverToken != null) {
                tokens.put(receiverToken);
            }

            if (receivers != null && receivers.size() > 0) {
                StringBuilder userNames = new StringBuilder();
                for (int i = 0; i < receivers.size(); i++) {
                    tokens.put(receivers.get(i).userToken);
                    userNames.append(receivers.get(i).firstName).append(" ").append(receivers.get(i).lastName).append("\n");
                }
                binding.firstCharText.setVisibility(View.GONE);
                binding.userEmailTextView.setVisibility(View.GONE);
                binding.userNameTextView.setText(userNames.toString());
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID) + "_" +
                    UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    exception.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Method to cancel invitation
     *
     * @param receiverToken is the receiver token
     **/
    private void cancelInvitation(String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();

            if (receiverToken != null) {
                tokens.put(receiverToken);
            }

            if (receivers != null && receivers.size() > 0) {
                for (User user : receivers) {
                    tokens.put(user.userToken);
                }
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    exception.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Method to send remote message
     *
     * @param remoteMessageBody is the remote message body that to be send
     * @param type              is the type of meeting invitation sent 'video/audio'
     **/
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                        Toast.makeText(
                                OutgoingMeetingInvitation.this,
                                "Invitation sent successfully",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        Toast.makeText(
                                OutgoingMeetingInvitation.this,
                                "Invitation cancelled",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    }
                } else {
                    Toast.makeText(
                            OutgoingMeetingInvitation.this,
                            response.message(),
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(
                        OutgoingMeetingInvitation.this,
                        t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}