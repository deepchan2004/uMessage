package com.example.umessage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.umessage.adapter.ChatRecyclerAdapter;
import com.example.umessage.adapter.SearchUserRecyclerAdapter;
import com.example.umessage.model.ChatMessageModel;
import com.example.umessage.model.ChatroomModel;
import com.example.umessage.model.UserModel;
import com.example.umessage.utils.AndroidUtil;
import com.example.umessage.utils.FireBaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    String chatroomId;
    ChatroomModel chatroomModel;
    UserModel otherUser;
    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;
    ChatRecyclerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //get userModel

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FireBaseUtil.getChatoomId(FireBaseUtil.currentUserId(),otherUser.getUserId());
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
        FireBaseUtil.getOtherProfilePicStorageReference(otherUser.getUserId()).getDownloadUrl().addOnCompleteListener(t->{
            if(t.isSuccessful())
            {
                Uri uri = t.getResult();
                AndroidUtil.setProfilePic(this,uri,imageView);
            }
        });

        backBtn.setOnClickListener((v)->{
            onBackPressed();
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener(v->{
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty())
            {
                return;
            }
            else
            {
                sendMessageToUser(message);
            }
        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    void setupChatRecyclerView()
    {
        Query query = FireBaseUtil.getChatRoomMessageReference(chatroomId).orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>().setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options,getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message)
    {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FireBaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FireBaseUtil.getChatRoomReference(chatroomId).set(chatroomModel);
        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FireBaseUtil.currentUserId(),Timestamp.now());
        FireBaseUtil.getChatRoomMessageReference(chatroomId).add(chatMessageModel).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if(task.isSuccessful())
                {
                    messageInput.setText(""); //To clear the message from edittext once sent
                    sendNotification(message);
                }
            }
        });
    }
    void getOrCreateChatroomModel()
    {
        FireBaseUtil.getChatRoomReference(chatroomId).get().addOnCompleteListener(task-> {
            if(task.isSuccessful())
            {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if(chatroomModel==null)
                {
                    //First time chatting
                    chatroomModel = new ChatroomModel(chatroomId, Arrays.asList(FireBaseUtil.currentUserId(),otherUser.getUserId()), Timestamp.now(),"");
                    FireBaseUtil.getChatRoomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }

    void sendNotification(String message)
    {
        FireBaseUtil.currentUserDetails().get().addOnCompleteListener(task->{
            if(task.isSuccessful())
            {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try{
                    JSONObject jsonObject = new JSONObject();
                    JSONObject notificationObj = new JSONObject();
                    JSONObject dataObj = new JSONObject();
                    notificationObj.put("title",currentUser.getUsername());
                    notificationObj.put("body",message);
                    dataObj.put("userId",currentUser.getUserId());

                    jsonObject.put("notification",notificationObj);
                    jsonObject.put("data",dataObj);
                    jsonObject.put("to",otherUser.getFcmToken());

                    callApi(jsonObject);
                }catch(Exception e)
                {

                }
            }
        });
    }

    void callApi(JSONObject jsonObject)
    {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(),JSON);
        Request request = new Request.Builder().url(url).post(body).header("Authorization","Bearer AAAAc0gvMvg:APA91bFGaTRZ49r9G8mD-7zm2n1zr1nd7g2rD5JPSthfKRrdSsslkQzYxR-HC28rlvedZqxO6pxA5YVWwr2ugybZ6iYwGdPFg7PdleN5iEzNVM37nBuQB65UxmzP608QXS8u-C7X8-kp").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }

}