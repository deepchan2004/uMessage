package com.example.umessage.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Document;

import java.text.SimpleDateFormat;
import java.util.List;

public class FireBaseUtil {

    public static String currentUserId()
    {
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn()
    {
        if(currentUserId()!=null)
        {
            return true;
        }
        return false;
    }
    public static DocumentReference currentUserDetails()
    {
        return FirebaseFirestore.getInstance().collection("users").document(currentUserId());
    }

    public static CollectionReference allUserCollectionReference(){
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static DocumentReference getChatRoomReference(String chatroomId)
    {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
    }

    public static CollectionReference getChatRoomMessageReference(String chatroomId)
    {
        return getChatRoomReference(chatroomId).collection("chats");
    }

    //To ensure that Uids are same either side
    public static String getChatoomId(String userId1,String userId2)
    {
        if(userId1.hashCode()<userId2.hashCode())
        {
            return userId1+"_"+userId2;
        }
        else
        {
            return userId2+"_"+userId1;
        }
    }

    public static CollectionReference allChatrromCollectionReference(){
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }


    public static DocumentReference getOtherUserFromChatroom(List<String> userIds)
    {
        if(userIds.get(0).equals(FireBaseUtil.currentUserId()))
        {
            return allUserCollectionReference().document(userIds.get(1));
        }
        else
        {
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    public static String timestampToString(Timestamp timestamp)
    {
        return new SimpleDateFormat("HH:MM").format(timestamp.toDate());
    }

    public static void logout()
    {
        FirebaseAuth.getInstance().signOut();
    }

    public static StorageReference getCurrentProfilePicStorageReference(){
        return FirebaseStorage.getInstance().getReference().child("profile_pic").child(FireBaseUtil.currentUserId());
    }

    public static StorageReference getOtherProfilePicStorageReference(String otherUserId){
        return FirebaseStorage.getInstance().getReference().child("profile_pic").child(otherUserId);
    }
}
