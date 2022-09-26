package edu.stevens.cs522.chatserver.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import edu.stevens.cs522.chatserver.entities.Message;

// TODO add annotations for Repository pattern

public interface MessageDao {

    /**
     * Get all messages in the database.
     * @return
     */

    public LiveData<List<Message>> fetchAllMessages();

    /**
     * Get all the messages for a particular peer.
     * @param peerName
     * @return
     */

    public LiveData<List<Message>> fetchMessagesFromPeer(String peerName);

    /**
     * Add a new message to the database.
     * @param message
     */

    public void persist(Message message);

}
