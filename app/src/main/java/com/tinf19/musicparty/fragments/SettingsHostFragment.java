package com.tinf19.musicparty.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

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

import org.json.JSONException;
import org.json.JSONObject;

import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.WHITE;

public class SettingsHostFragment extends Fragment {

    private static final String TAG = SettingsHostFragment.class.getName();
    private EditText changePartyName;
    private TextView ipAddressTextView;
    private TextView passwordTextView;
    private GetServerSettings getServerSettings;
    private String partyName = "Music Party";
    private Bitmap bitmap;

    public interface GetServerSettings {
        String getIpAddress();
        String getPassword();
        void setNewPartyName(String newPartyName);
    }

    public SettingsHostFragment(GetServerSettings getServerSettings) {
        this.getServerSettings = getServerSettings;
    }

    public SettingsHostFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(ipAddressTextView != null) {
            String text = getString(R.string.text_ip_address) + ": " + getServerSettings.getIpAddress();
            Log.d(TAG, "onStart IpAddress: " + text);
            ipAddressTextView.setText(text);
        }
        if(passwordTextView != null) {
            String text = getString(R.string.app_password) + ": " + getServerSettings.getPassword();
            Log.d(TAG, "onStart Password: " + text);
            passwordTextView.setText(text);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_host, container, false);

        ImageView qrCodeImageView = view.findViewById(R.id.qrConnectionSettingsImageView);

        JSONObject json = new JSONObject();
        try {
            json.put("ipaddress", getServerSettings.getIpAddress());
            json.put("password", getServerSettings.getPassword());
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
            shareAddressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "*Verbindung zu " + partyName + ":* \n" + ipAddressTextView.getText() + "\n" + passwordTextView.getText());
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                }
            });
        }

        ImageButton shareQRButton = view.findViewById(R.id.shareQRButtonSettingsImageButton);
        if(shareQRButton != null) {
            shareQRButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String bitmapPath = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap,"title", null);
                    Uri bitmapUri = Uri.parse(bitmapPath);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.setType("image/png");
                    Intent shareIntent = Intent.createChooser(sendIntent, "Share");
                    startActivity(shareIntent);
                }
            });
        }

        ImageButton shareLinkButton = view.findViewById(R.id.shareLinkButtonSettingsImageButton);
        if(shareLinkButton != null) {
            shareLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "*Verbindung zu " + partyName + ":* \n"  + getURI());
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                }
            });
        }

        Button savePartyNameButton = view.findViewById(R.id.savePartyNameButton);
        if (savePartyNameButton != null) {
            savePartyNameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changePartyName = view.findViewById(R.id.changePartyNameEditText);
                    if(changePartyName != null && !changePartyName.getText().toString().equals("")) {
                        String newPartyName = changePartyName.getText().toString();
                        Log.d(TAG, "onClick: new Party Name set to: " + newPartyName);
                        partyName = newPartyName;
                        getServerSettings.setNewPartyName(newPartyName);
                        Toast.makeText(getContext(), "Der Partyname wurde auf " + newPartyName + " geändert.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        return view;
    }


    public String getURI() {
        return "http://musicparty.join?" +
                Constants.ADDRESS + "=" + getServerSettings.getIpAddress() + "&" +
                Constants.PASSWORD + "=" + getServerSettings.getPassword();
    }
}