/*********************************************************************

    Chat server: accept chat messages from clients.
    
    Sender name and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2017 Stevens Institute of Technology

**********************************************************************/
package edu.stevens.cs522.chatserver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.stevens.cs522.base.DatagramSendReceive;
import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.databases.ChatDatabase;
import edu.stevens.cs522.chatserver.databases.MessageDao;
import edu.stevens.cs522.chatserver.databases.PeerDao;
import edu.stevens.cs522.chatserver.entities.Message;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.MessagesAdapter;

public class ChatServerActivity extends FragmentActivity implements OnClickListener {

    final static public String TAG = ChatServerActivity.class.getCanonicalName();

    public final static String SENDER_NAME = "name";

    public final static String CHATROOM = "room";

    public final static String MESSAGE_TEXT = "text";

    public final static String TIMESTAMP = "timestamp";

    public final static String LATITUDE = "latitude";

    public final static String LONGITUDE = "longitude";

    /*
     * Socket used both for sending and receiving
     */
    private DatagramSendReceive serverSocket;
//  private DatagramSocket serverSocket;


    /*
     * True as long as we don't get socket errors
     */
    private boolean socketOK = true;

    /*
     * Data access objects.
     */
    private ChatDatabase chatDatabase;

    private MessageDao messageDao;

    private PeerDao peerDao;

    private Button btnNext;


    /*
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_messages);

        /**
         * Let's be clear, this is a HACK to allow you to do network communication on the messages thread.
         * This WILL cause an ANR, and is only provided to simplify the pedagogy.  We will see how to do
         * this right in a future assignment (using a Service managing background threads).
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            /*
             * Get port information from the resources.
             */
            int port = getResources().getInteger(R.integer.app_port);

            // serverSocket = new DatagramSocket(port);

            serverSocket = new DatagramSendReceive(port);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot open socket", e);
        }

        Log.d(TAG, "Initializing the UI with no messages....");

        MessagesAdapter messagesAdapter = new MessagesAdapter(this);
        /*
         * List of messages and adapter.
         */
        ListView messageList = findViewById(R.id.message_list);
        messageList.setAdapter(messagesAdapter);

        Log.d(TAG, "Opening the database....");
        // TODO open the database
        // Note use getApplicationContext, do not make DB depend on UI!
        // Get database instance
        chatDatabase =  ChatDatabase.getInstance(ChatServerActivity.this);


        Log.d(TAG, "Querying the database asynchronously....");
        // TODO query the database asynchronously, registering an observer for the result.
        // Note: The adapter has a method for resetting the backing store.
        // Get dao
        messageDao = chatDatabase.messageDao();
        peerDao = chatDatabase.peerDao();
        // Query database
        List<Message> messages = messageDao.fetchAllMessages().getValue();
        //if there are no messages yet then the line above will come back null
        //but we need to be able to show that there are no messages yet and null will break the program so
        if (messages == null){
            messages = new ArrayList<>();
        }
        //set the elements that the adapter should show on the screen
        messagesAdapter.setElements(messages);

        //Since we are observing the fetchAllMessages, when a new message gets added to the table
        //the LiveData in our app will change so this will get fired off
        messageDao.fetchAllMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                //if our list of messages has changed we should set the elements in our
                //adapter to be the new list of messagesa, and also notify the UI
                //that the data has changed so it get's redrawn
                messagesAdapter.setElements(messages);
                messagesAdapter.notifyDataSetChanged();
            }
        });


        //we need the initial load to show some values so lets set the LiveData value
        //that way the observer above fires

        //NOTE: the LiveData class does not have setValue so cast this as MutableLiveData
        //messageDao.fetchAllMessages().setValue(messages);


        Log.d(TAG, "Binding the callback for the NEXT button....");
        // TODO bind the button for "next" to this activity as listener
        btnNext = findViewById(R.id.next);
        btnNext.setOnClickListener(this);


    }

    public void onClick(View v) {

        byte[] receiveData = new byte[1024];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        try {

            String sender = null;

            String room = null;

            String text = null;

            Date timestamp = null;

            Double latitude = null;

            Double longitude = null;

            /*
             * THere is an apparent bug in the emulator stack on Windows where
             * messages can arrive empty, we loop as a workaround.
             */

            while (sender == null) {

                Log.d(TAG, "Waiting for a message....");
                serverSocket.receive(receivePacket);
                Log.d(TAG, "Received a packet!");

                if (receivePacket.getLength() == 0) {
                    Log.d(TAG, "....zero-length packet, skipping....");
                    continue;
                }

                InetAddress address = receivePacket.getAddress();
                int port = receivePacket.getPort();
                Log.d(TAG, "Source IP Address: " + address + " , Port: " + port);

                String content = new String(receivePacket.getData(), 0, receivePacket.getLength());
                Log.d(TAG, "Message received: " + content);

                /*
                 * Parse the JSON object
                 */
                JsonReader rd = new JsonReader(new StringReader(content));

                rd.beginObject();
                if (SENDER_NAME.equals(rd.nextName())) {
                    sender = rd.nextString();
                }
                if (CHATROOM.equals(rd.nextName())) {
                    room = rd.nextString();
                }
                if (MESSAGE_TEXT.equals((rd.nextName()))) {
                    text = rd.nextString();
                }
                if (TIMESTAMP.equals(rd.nextName())) {
                    timestamp = new Date(rd.nextLong());
                }
                if (LATITUDE.equals(rd.nextName())) {
                    latitude = rd.nextDouble();
                }
                if (LONGITUDE.equals((rd.nextName()))) {
                    longitude = rd.nextDouble();
                }
                rd.endObject();

                rd.close();

            }

            /*
             * Add the sender to our list of senders
             */
            Peer peer = new Peer();
            peer.name = sender;
            peer.timestamp = timestamp;
            peer.latitude = latitude;
            peer.longitude = longitude;

            Message message = new Message();
            message.messageText = text;
            message.chatroom = room;
            message.sender = sender;
            message.timestamp = timestamp;
            message.latitude = latitude;
            message.longitude = longitude;

            /*
             * TODO upsert peer and insert message into the database
             */
            peerDao.upsert(peer);
            messageDao.persist(message);
            /*
             * End TODO
             *
             * The livedata for the messages should update via observer automatically.
             */

        } catch (Exception e) {

            Log.e(TAG, "Problems receiving packet: " + e.getMessage(), e);
            socketOK = false;
        }

    }

    /*
     * Close the socket before exiting application
     */
    public void closeSocket() {
        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
    }

    /*
     * If the socket is OK, then it's running
     */
    boolean socketIsOK() {
        return socketOK;
    }

    public void onDestroy() {
        super.onDestroy();
        chatDatabase = null;
        closeSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // TODO inflate a menu with PEERS option
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chatserver_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.peers) {
            // TODO PEERS provide the UI for viewing list of peers
            // The subactivity will query the database for the list of peers.
            Intent i = new Intent(this, ViewPeersActivity.class );
            startActivity(i);

        }
        return false;
    }

}