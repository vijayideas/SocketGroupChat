package com.vjy;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // LogCat tag
    private static final String TAG = ChatActivity.class.getSimpleName();

    private Button btnSend;
    private EditText inputMsg;


    // Chat messages list adapter
    private MessagesListAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;

    private Utils utils;
    // Client name
    private String name = null;

    private WebSocketClient mWebSocketClient;

    // JSON flags to identify the kind of JSON response
    private static final String TAG_SELF = "self", TAG_NEW = "new",
            TAG_MESSAGE = "message", TAG_EXIT = "exit";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = (Button) findViewById(R.id.btnSend);
        inputMsg = (EditText) findViewById(R.id.inputMsg);
        listViewMessages = (ListView) findViewById(R.id.list_view_messages);

        utils = new Utils(getApplicationContext());

        // Getting the person name from previous screen
        Intent i = getIntent();
        name = i.getStringExtra("name");

        btnSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Sending message to web socket server
                sendMessageToServer(utils.getSendMessageJSON(inputMsg.getText()
                        .toString()));

                // Clearing the input filed once message was sent
                inputMsg.setText("");
            }
        });

        listMessages = new ArrayList<Message>();

        adapter = new MessagesListAdapter(this, listMessages);
        listViewMessages.setAdapter(adapter);

        URI uri;
        try {
            uri = new URI(WsConfig.URL_WEBSOCKET + name);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        Draft draft = new Draft_17();

        mWebSocketClient = new WebSocketClient(uri, draft) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                showToast("connect");
//                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }



            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseMessage(message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }




    /**
     * Method to send message to web socket server
     * */
    private void sendMessageToServer(String message) {
        if (mWebSocketClient != null) {
            mWebSocketClient.send(message);
        }
    }


    /**
     * Parsing the JSON message received from server The intent of message will
     * be identified by JSON node 'flag'. flag = self, message belongs to the
     * person. flag = new, a new person joined the conversation. flag = message,
     * a new message received from server. flag = exit, somebody left the
     * conversation.
     * */
    private void parseMessage(final String msg) {

        try {
            JSONObject jObj = new JSONObject(msg);

            // JSON node 'flag'
            String flag = jObj.getString("flag");

            // if flag is 'self', this JSON contains session id
            if (flag.equalsIgnoreCase(TAG_SELF)) {

                String sessionId = jObj.getString("sessionId");

                // Save the session id in shared preferences
                utils.storeSessionId(sessionId);

                Log.e(TAG, "Your session id: " + utils.getSessionId());

            } else if (flag.equalsIgnoreCase(TAG_NEW)) {
                // If the flag is 'new', new person joined the room
                String name = jObj.getString("name");
                String message = jObj.getString("message");

                // number of people online
                String onlineCount = jObj.getString("onlineCount");

                showToast(name + message + ". Currently " + onlineCount
                        + " people online!");

            } else if (flag.equalsIgnoreCase(TAG_MESSAGE)) {
                // if the flag is 'message', new message received
                String fromName = name;
                String message = jObj.getString("message");
                String sessionId = jObj.getString("sessionId");
                boolean isSelf = true;

                // Checking if the message was sent by you
                if (!sessionId.equals(utils.getSessionId())) {
                    fromName = jObj.getString("name");
                    isSelf = false;
                }

                Message m = new Message(fromName, message, isSelf);

                // Appending the message to chat list
                appendMessage(m);

            } else if (flag.equalsIgnoreCase(TAG_EXIT)) {
                // If the flag is 'exit', somebody left the conversation
                String name = jObj.getString("name");
                String message = jObj.getString("message");

                showToast(name + message);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    protected void onDestroy() {
        super.onDestroy();

        if(mWebSocketClient != null){
            mWebSocketClient.close();
        }
    }

    /**
     * Appending message to list view
     * */
    private void appendMessage(final Message m) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                listMessages.add(m);

                adapter.notifyDataSetChanged();

                // Playing device's notification
                playBeep();
            }
        });
    }

    private void showToast(final String message) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_LONG).show();
            }
        });

    }


    /**
     * Plays device's default notification sound
     * */
    public void playBeep() {

        try {
            Uri notification = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                    notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }



}