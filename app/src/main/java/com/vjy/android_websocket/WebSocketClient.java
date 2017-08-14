package com.vjy.android_websocket;


import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Created by Vijay Kumar on 28-07-2017.
 * @author Vijay Kumar
 */

public class WebSocketClient extends org.java_websocket.client.WebSocketClient{
    private URI mURI;
    private Listener mListener;
    private static Draft draft = new Draft_17();
    private boolean mConnected;


    public WebSocketClient(URI serverURI,Listener mListener) {
        super(serverURI,draft);

        this.mListener = mListener;
        mConnected = false;
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {
        mListener.onConnect();
        mConnected = true;
    }

    @Override
    public void onMessage(String message) {
        this.mListener.onMessage(message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        super.onMessage(bytes);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.mListener.onDisconnect(code,reason);
        mConnected = false;
    }

    @Override
    public void onError(Exception ex) {
        this.mListener.onError(ex);
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public void disconnect() {
        this.close();
    }



    /**
     * Starts a background thread that attempts and maintains a WebSocket
     * connection to the URI specified in the constructor or via <var>setURI</var>.
     * <var>setURI</var>.
     */
    @Override
    public void connect() {
        super.connect();
    }

    public interface Listener {
        void onConnect();
        void onMessage(String message);
        void onDisconnect(int code, String reason);
        void onError(Exception error);
    }
}
