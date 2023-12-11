package com.example.umessage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.umessage.model.UserModel;
import com.example.umessage.utils.AndroidUtil;
import com.example.umessage.utils.FireBaseUtil;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if(FireBaseUtil.isLoggedIn() && getIntent().getExtras()!=null)
        {
            //from notification

            String userId = getIntent().getExtras().getString("userId");
            FireBaseUtil.allUserCollectionReference().document(userId).get().addOnCompleteListener(task->{
                if(task.isSuccessful())
                {
                    UserModel model = task.getResult().toObject(UserModel.class);

                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(mainIntent);
                    Intent intent = new Intent(this, ChatActivity.class);
                    AndroidUtil.passUserModelAsIntent(intent,model);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
        else {


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (FireBaseUtil.isLoggedIn()) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginPhoneActivity.class));
                    }
                    finish();
                }
            }, 1000);
        }


    }
}