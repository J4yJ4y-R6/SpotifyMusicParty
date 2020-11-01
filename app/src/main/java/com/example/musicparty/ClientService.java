package com.example.musicparty;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;

import com.example.musicparty.music.Artist;
import com.example.musicparty.music.Track;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.musicparty.App.CHANNEL_ID;

public class ClientService extends Service {


    private static final String NAME = ClientService.class.getName();
    private static final int PORT = 1403;
    private final IBinder mBinder = new LocalBinder();
    private ClientThread clientThread;
    private Socket clientSocket;
    private boolean first = true;
    private List<Track> queue = new ArrayList<>();

    public class LocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    public ClientService() {
    }

    public void logHi() {
        Log.d(NAME, "HELLO WORLD");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, PartyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Party")
                .setContentText("You are in a Music Party")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(first) connect(intent.getStringExtra(Constants.ADDRESS), intent.getStringExtra(Constants.PASSWORD));
        //password = intent.getStringExtra("password");

        return START_NOT_STICKY;
    }

    public void connect(String ipAddress, String password){
        clientThread = new ClientThread(ipAddress, password);
        clientThread.start();
    }

    public ClientThread getClientThread() {
        return clientThread;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    class ClientThread extends Thread {

        private String address;
        private BufferedReader input;
        private DataOutputStream out;
        private String password;
        private String line;
        private String username = "Test";
        private String partyName;

        public ClientThread(String address, String password) {
            this.address = address;
            this.password = password;
        }

        public void sendMessage(Commands commands, String message) throws IOException {
            Log.d(NAME, String.format("~%s~%s\n\r" , commands.toString(), message));
            out.writeBytes(String.format("~%s~%s\n\r" , commands.toString(), message));
            out.flush();
        }

        @Override
        public void run() {
            try {
                Log.d(NAME, "Try to login to " + address + ":" + PORT + " with password " + this.password);
                clientSocket = new Socket(this.address, PORT);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new DataOutputStream(clientSocket.getOutputStream());
                sendMessage(Commands.LOGIN, this.username + "~" + this.password);
                Log.d(NAME, "Connect successful");
                while (true)  {
                    line = input.readLine();
                    if (line != null) {
                        String [] parts = line.split("~");
                        String attribute = "";
                        if (parts.length > 2)
                            attribute = parts[2];
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            switch (command) {
                                case LOGIN:
                                    partyName = attribute;
                                    Log.d(NAME, partyName);
                                    break;
                                case QUIT:
                                    Log.d(NAME, "Server has been closed");
                                    input.close();
                                    out.close();
                                    clientSocket.close();
                                    return;
                                case QUEUE:
                                    queue.add(new Track(attribute));
                                    Log.d(NAME, attribute);
                                    break;
                                case PLAYING:
                                    Track track = new Track(attribute);
                                    Log.d(NAME, "Playing: " + attribute);
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(NAME, e.getMessage(), e);

            }
        }
    }
}