package in.codewithmike.droidmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.codewithmike.droidmeet.R;
import in.codewithmike.droidmeet.listeners.UsersListener;
import in.codewithmike.droidmeet.models.User;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> {

    private List<User> users;
    private UsersListener usersListener;
    private List<User> selectedUsers;

    public UsersAdapter(List<User> users, UsersListener usersListener) {
        this.users = users;
        this.usersListener = usersListener;
        selectedUsers = new ArrayList<>();
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UsersViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_user,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UsersViewHolder extends RecyclerView.ViewHolder {

        TextView firstCharText, usernameText, userEmail;
        ImageView imageAudioMeeting, imageVideoMeeting, imageSelected;
        ConstraintLayout userContainer;

        UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            userContainer = itemView.findViewById(R.id.user_container);
            imageSelected = itemView.findViewById(R.id.image_selected);
            firstCharText = itemView.findViewById(R.id.first_char_text);
            usernameText = itemView.findViewById(R.id.user_name_text_view);
            userEmail = itemView.findViewById(R.id.user_email_text_view);
            imageAudioMeeting = itemView.findViewById(R.id.image_audio_meeting);
            imageVideoMeeting = itemView.findViewById(R.id.image_video_meeting);
        }

        void setUserData(User user) {
            firstCharText.setText(user.firstName.substring(0, 1));
            usernameText.setText(String.format("%s %s", user.firstName, user.lastName));
            userEmail.setText(user.userEmail);

            imageAudioMeeting.setOnClickListener(view -> usersListener.initiateAudioMeeting(user));
            imageVideoMeeting.setOnClickListener(view -> usersListener.initiateVideoMeeting(user));

            userContainer.setOnLongClickListener(view -> {
                if (imageSelected.getVisibility() != View.VISIBLE) {
                    selectedUsers.add(user);
                    imageSelected.setVisibility(View.VISIBLE);
                    imageVideoMeeting.setVisibility(View.GONE);
                    imageAudioMeeting.setVisibility(View.GONE);
                    usersListener.onMultipleUsersAction(true);
                }
                return true;
            });

            userContainer.setOnClickListener(view -> {
                if (imageSelected.getVisibility() == View.VISIBLE) {
                    selectedUsers.remove(user);
                    imageSelected.setVisibility(View.GONE);
                    imageVideoMeeting.setVisibility(View.VISIBLE);
                    imageAudioMeeting.setVisibility(View.VISIBLE);
                    if (selectedUsers.size() == 0) {
                        usersListener.onMultipleUsersAction(false);
                    }
                } else {
                    if (selectedUsers.size() > 0) {
                        selectedUsers.add(user);
                        imageSelected.setVisibility(View.VISIBLE);
                        imageVideoMeeting.setVisibility(View.GONE);
                        imageAudioMeeting.setVisibility(View.GONE);
                    }
                }
            });
        }

    }
}
