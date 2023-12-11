package com.example.umessage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.umessage.model.UserModel;
import com.example.umessage.utils.AndroidUtil;
import com.example.umessage.utils.FireBaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment {

    ImageView profilePic;
    EditText usernameInput;
    EditText phoneInput;

    Button updateProfileButton;
    ProgressBar progressBar;
    TextView logoutBtn;

    UserModel currentUserModel;

    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
    public ProfileFragment() {

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result->{
            if(result.getResultCode()== Activity.RESULT_OK)
            {
                Intent data = result.getData();
                if(data!=null && data.getData()!=null)
                {
                    selectedImageUri = data.getData();
                    AndroidUtil.setProfilePic(getContext(),selectedImageUri,profilePic);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.profile_username);
        phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileButton = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);

        getUserData();

        updateProfileButton.setOnClickListener(v->{
            updateBtnClick();
        });

        logoutBtn.setOnClickListener(v->{
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        FireBaseUtil.logout();
                        Intent intent = new Intent(getContext(),SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
            });

        });

        profilePic.setOnClickListener((v)->{
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512).createIntent(new Function1<Intent, Unit>() {
                @Override
                public Unit invoke(Intent intent) {
                    imagePickLauncher.launch(intent);
                    return null;
                }
            });
        });
        return view;
    }

    void updateBtnClick(){
        String newUsername = usernameInput.getText().toString();
        if(newUsername.isEmpty() || newUsername.length()<3)
        {
            usernameInput.setError("Username must have at least 3 characters");
            return;
        }
        currentUserModel.setUsername(newUsername);
        setInProgress(true);

        if(selectedImageUri!=null) {
            FireBaseUtil.getCurrentProfilePicStorageReference().putFile(selectedImageUri).addOnCompleteListener(task->{
                updateToFirestore();
            });
        }
        else{
            updateToFirestore();
        }

    }

    void updateToFirestore(){
        FireBaseUtil.currentUserDetails().set(currentUserModel).addOnCompleteListener(task->{
            setInProgress(false);
            if(task.isSuccessful())
            {
                AndroidUtil.showToast(getContext(),"Profile Updated Successfully");
            }
            else{
                AndroidUtil.showToast(getContext(),"Profile Updated Failed");
            }
        });
    }
    void getUserData()
    {
        setInProgress(true);

        FireBaseUtil.getCurrentProfilePicStorageReference().getDownloadUrl().addOnCompleteListener(task->{
            if(task.isSuccessful())
            {
                Uri uri = task.getResult();
                AndroidUtil.setProfilePic(getContext(),uri,profilePic);
            }
        });
        FireBaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            currentUserModel = task.getResult().toObject(UserModel.class);
            usernameInput.setText(currentUserModel.getUsername());
            phoneInput.setText(currentUserModel.getPhone());
        });
    }

    void setInProgress(boolean inProgress)
    {
        if(inProgress)
        {
            progressBar.setVisibility(View.VISIBLE);
            updateProfileButton.setVisibility(View.GONE);
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            updateProfileButton.setVisibility(View.VISIBLE);
        }
    }
}