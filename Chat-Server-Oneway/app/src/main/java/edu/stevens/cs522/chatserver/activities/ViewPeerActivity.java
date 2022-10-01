package edu.stevens.cs522.chatserver.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.databases.ChatDatabase;
import edu.stevens.cs522.chatserver.entities.Message;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.SimpleArrayAdapter;

/**
 * Created by dduggan.
 */

public class ViewPeerActivity extends FragmentActivity {

    public static final String PEER_KEY = "peer";

    private ChatDatabase chatDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_peer);

        Peer peer = getIntent().getParcelableExtra(PEER_KEY);
        if (peer == null) {
            throw new IllegalArgumentException("Expected peer as intent extra");
        }

        // TODO Set the fields of the UI
        TextView userName = findViewById(R.id.view_user_name);
        TextView lastSeen = findViewById(R.id.view_timestamp);
        TextView location = findViewById(R.id.view_location);

        userName.setText( getString(R.string.view_user_name, peer.name)  );
        lastSeen.setText( getString(R.string.view_timestamp, formatTimestamp(peer.timestamp))  );
        location.setText( getString(R.string.view_location, peer.latitude,peer.longitude)  );
        // End TODO

        SimpleArrayAdapter<Message> messagesAdapter = new SimpleArrayAdapter<>(this);
        ListView messagesList = findViewById(R.id.message_list);
        messagesList.setAdapter(messagesAdapter);

        chatDatabase = ChatDatabase.getInstance(getApplicationContext());

        /*
         * TODO query the database asynchronously for the messages just for this peer.
         */
        List<Message> peerMessages = chatDatabase.messageDao().fetchMessagesFromPeer(peer.name).getValue();
        if (peerMessages == null){
            peerMessages = new ArrayList<>();
        }
        //give the adapter the information to show
        messagesAdapter.setElements(peerMessages);
        messagesAdapter.notifyDataSetChanged();

        chatDatabase.messageDao().fetchMessagesFromPeer(peer.name).observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messagesAdapter.setElements(messages);
                messagesAdapter.notifyDataSetChanged();
            }
        });

    }

    //TODO PUT THIS BACK IN
    private static String formatTimestamp(Date timestamp) {
        LocalDateTime dateTime = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return dateTime.format(formatter);
       /* SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return simpleDateFormat.format(timestamp);*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chatDatabase = null;
    }

}