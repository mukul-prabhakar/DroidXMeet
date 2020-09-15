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

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import in.codewithmike.droidmeet.R;
import in.codewithmike.droidmeet.databinding.ActivityIncomingInvitationBinding;
import in.codewithmike.droidmeet.network.ApiClient;
import in.codewithmike.droidmeet.network.ApiService;
import in.codewithmike.droidmeet.utilities.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    private String meetingType = null;
    /*** Method to listen response from receiver **/
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    Toast.makeText(context, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityIncomingInvitationBinding binding =
                ActivityIncomingInvitationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                binding.imageMeetingType.setImageResource(R.drawable.icon_video_cam);
            } else if (meetingType.equals("audio")) {
                binding.imageMeetingType.setImageResource(R.drawable.icon_call);
            }
        }

        String userFirstName = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (userFirstName != null) {
            binding.firstCharText.setText(userFirstName.substring(0, 1));

        }

        binding.userNameTextView.setText(String.format("%s %s",
                userFirstName, getIntent().getStringExtra(Constants.KEY_LAST_NAME)));

        binding.userEmailTextView.setText(getIntent().getStringExtra(Constants.KEY_EMAIL));

        binding.acceptInvitationButton.setOnClickListener(view1 -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
            //onBackPressed();
        });

        binding.rejectInvitationButton.setOnClickListener(view1 -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_REJECTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
            // onBackPressed();
        });
    }

    /**
     * Method to send remote message
     *
     * @param type          is the type of meeting invitation sent 'video/audio'
     * @param receiverToken is the token who send the invitation
     **/
    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), type);

        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        try {
                            URL serverURL = new URL("https://meet.jit.si");
                            JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                            builder.setServerURL(serverURL);
                            builder.setWelcomePageEnabled(false);
                            builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                            if (meetingType.equals("audio")) {
                                builder.setVideoMuted(true);
                            }
                            JitsiMeetActivity.launch(IncomingInvitationActivity.this, builder.build());
                            finish();
                        } catch (Exception exception) {
                            Toast.makeText(
                                    IncomingInvitationActivity.this,
                                    exception.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(
                                IncomingInvitationActivity.this,
                                "Invitation Rejected",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    }
                } else {
                    Toast.makeText(
                            IncomingInvitationActivity.this,
                            response.message(),
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(
                        IncomingInvitationActivity.this,
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