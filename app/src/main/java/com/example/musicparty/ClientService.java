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

    public interface ClientCallback {
        void updateView(List<Track> tracks);
    }

    private static final String NAME = ClientService.class.getName();
    private static final String HOST = "api.spotify.com";
    private static final int PORT = 1403;
    private static String token;
    private final IBinder mBinder = new LocalBinder();
    private int limit = 10;
    private String type = "track";
    private Thread clientThread;
    private Socket clientSocket;
    private List<Track> tracks = new ArrayList<>();
    private boolean first = true;
    private ClientCallback clientCallback;

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
        Intent notificationIntent = new Intent(this, HostActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Party")
                .setContentText("A music party is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(first) connect(intent.getStringExtra(Constants.ADDRESS), intent.getStringExtra(Constants.PASSWORD));
        token = intent.getStringExtra("token");
        //password = intent.getStringExtra("password");

        return START_NOT_STICKY;
    }

    public void connect(String ipAddress, String password){
        clientThread = new Thread(new ClientThread(ipAddress, password));
        clientThread.start();
    }

    public List<Track> getTracks() {
        return tracks;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCallback(ClientCallback clientCallback) {
        this.clientCallback = clientCallback;
    }

    public void search(String query){
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("type", type)
                .addQueryParameter("limit", String.valueOf(limit))
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Log.d(NAME, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful.");
                }
                final String data = response.body().string();

                // Read data in the worker thread
                extractSongs(data);
            }
        });
    }

    public void extractSongs(String data) {
        try {
            tracks.clear();
            JSONObject jsonObject = new JSONObject(data);
            jsonObject = jsonObject.getJSONObject("tracks");
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject track = jsonArray.getJSONObject(i);
                JSONArray artists = track.getJSONArray("artists");
                Artist[] array = new Artist[artists.length()];
                for(int j = 0; j < array.length; j++) {
                    JSONObject artist = artists.getJSONObject(j);
                    array[j] = new Artist(artist.getString("id"), artist.getString("name"));
                }
                String image = track
                        .getJSONObject("album")
                        .getJSONArray("images")
                        .getJSONObject(2)
                        .getString("url");
                tracks.add(
                        new Track(
                                track.getString("id"),
                                track.getString("name"), array,
                                image,
                                track.getInt("duration_ms")
                        ));
                Log.d(NAME, tracks.get(i).toString());
            }
            clientCallback.updateView(tracks);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    class ClientThread implements Runnable {

        private String address;
        private BufferedReader input;
        private DataOutputStream out;
        private String password;
        private String line;

        public ClientThread(String address, String password) {
            this.address = address;
            this.password = password;
        }

        @Override
        public void run() {
            try {
                Log.d(NAME, "Try to login to " + address + ":" + PORT + " with password " + this.password);
                clientSocket = new Socket(this.address, PORT);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new DataOutputStream(clientSocket.getOutputStream());
                out.writeBytes("~LOGIN~" + this.password + "\n\r");
                out.flush();
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
                                    Log.d(NAME, attribute);
                                    break;
                                case QUIT:
                                    clientSocket.close();
                                    return;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(NAME, e.getMessage(), e);

            }
        }
    }
}