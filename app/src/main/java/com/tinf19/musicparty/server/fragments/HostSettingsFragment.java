package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.HostVoting;

import org.json.JSONException;
import org.json.JSONObject;

import static android.graphics.Color.WHITE;

/**
 * Fragment where the host can change the settings of the party and share the connection variables
 * via link, qr-code or plain text.
 * Also he can change the type of the party. There are currently two types of partys
 * <ol>
 *     <li>All-In: Every song send to the queue is added.</li>
 *     <li>Voting: Each song send to the queue starts a new
 *     {@link HostVoting} where everybody can vote whether they want to add
 *     it or not. The same kind of voting is called when someone wants to skip the current track.
 *     </li>
 * </ol>
 * There will be more options in the future.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostSettingsFragment extends Fragment {

    private static final String TAG = HostSettingsFragment.class.getName();
    private EditText changePartyName;
    private TextView ipAddressTextView;
    private TextView passwordTextView;
    private HostSettingsCallback hostSettingsCallback;
    private String partyName = "Music Party";
    /**
     * A bitmap for the connection QR-Code.
     */
    private Bitmap bitmap;

    public interface HostSettingsCallback {
        String getIpAddress();
        String getPassword();
        void setNewPartyName(String newPartyName);
    }

    /**
     * Constructor to set the callback
     * @param hostSettingsCallback Communication callback for
     *                             {@link com.tinf19.musicparty.server.HostActivity}.
     */
    public HostSettingsFragment(HostSettingsCallback hostSettingsCallback) { this.hostSettingsCallback = hostSettingsCallback; }

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public HostSettingsFragment() { }



    //Android lifecycle methods

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.PARTYNAME, partyName);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "set connection information");
        if(ipAddressTextView != null) {
            String text = getString(R.string.text_ip_address) + ": " + hostSettingsCallback.getIpAddress();
            ipAddressTextView.setText(text);
        }
        if(passwordTextView != null) {
            String text = getString(R.string.app_password) + ": " + hostSettingsCallback.getPassword();
            passwordTextView.setText(text);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostSettingsCallback) {
            hostSettingsCallback = (HostSettingsCallback) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if(savedInstanceState != null) {
            partyName = savedInstanceState.getString(Constants.PARTYNAME, "MusicParty");
        }

        View view = inflater.inflate(R.layout.fragment_host_settings, container, false);

        ImageView qrCodeImageView = view.findViewById(R.id.qrConnectionSettingsImageView);

        JSONObject json = new JSONObject();
        try {
            Log.d(TAG, "generating qr code with connection information");
            json.put("ipaddress", hostSettingsCallback.getIpAddress());
            json.put("password", hostSettingsCallback.getPassword());
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(json.toString(), BarcodeFormat.QR_CODE, 200, 200);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            for(int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x,y) ? ContextCompat.getColor(getContext(), R.color.button_green) : WHITE;
                }
            }

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            if(qrCodeImageView != null) qrCodeImageView.setImageBitmap(bitmap);
        } catch (JSONException | WriterException e) {
            e.printStackTrace();
        }

        ipAddressTextView = view.findViewById(R.id.ipAddressSettingsTextView);
        passwordTextView = view.findViewById(R.id.passwordSettingsTextView);

        ImageButton shareAddressButton = view.findViewById(R.id.shareButtonSettingsImageButton);
        if(shareAddressButton != null) {
            shareAddressButton.setOnClickListener(v -> {
                Log.d(TAG, "share connection information as text");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "*Verbindung zu " + partyName + ":* \n" + ipAddressTextView.getText() + "\n" + passwordTextView.getText());
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            });
        }

        ImageButton shareQRButton = view.findViewById(R.id.shareQRButtonSettingsImageButton);
        if(shareQRButton != null) {
            shareQRButton.setOnClickListener(v -> {
                Log.d(TAG, "share connection information as QR-Code");
                String bitmapPath = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap,"title", null);
                Uri bitmapUri = Uri.parse(bitmapPath);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sendIntent.setType("image/png");
                Intent shareIntent = Intent.createChooser(sendIntent, "Share");
                startActivity(shareIntent);
            });
        }

        ImageButton shareLinkButton = view.findViewById(R.id.shareLinkButtonSettingsImageButton);
        if(shareLinkButton != null) {
            shareLinkButton.setOnClickListener(v -> {
                Log.d(TAG, "share connection information as a link");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "*Verbindung zu " + partyName + ":* \n"  + getURI());
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            });
        }

        Button savePartyNameButton = view.findViewById(R.id.savePartyNameButton);
        if (savePartyNameButton != null) {
            savePartyNameButton.setOnClickListener(v -> {
                changePartyName = view.findViewById(R.id.changePartyNameEditText);
                if(changePartyName != null && !changePartyName.getText().toString().equals("")) {
                    String newPartyName = changePartyName.getText().toString();
                    Log.d(TAG, "new Party Name set to: " + newPartyName);
                    partyName = newPartyName;
                    hostSettingsCallback.setNewPartyName(newPartyName);
                    Toast.makeText(getContext(), "Der Partyname wurde auf " + newPartyName + " geändert.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return view;
    }


    /**
     * @return Get the join link with the connection variables.
     */
    public String getURI() {
        Log.d(TAG, "generating join-link with connection information");
        return "http://musicparty.join?" +
                Constants.ADDRESS + "=" + hostSettingsCallback.getIpAddress() + "&" +
                Constants.PASSWORD + "=" + hostSettingsCallback.getPassword();
    }
}