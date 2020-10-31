package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.musicparty.databinding.ActivityClientBinding;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    ActivityClientBinding binding;
    private static final String NAME = ClientActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void nextPage(View view) {
        Intent intent = new Intent(this, PartyActivity.class);
        intent.putExtra(Constants.TOKEN, getIntent().getStringExtra("token"));
        intent.putExtra(Constants.PASSWORD, binding.etPassword.getText().toString());
        intent.putExtra(Constants.ADDRESS, binding.etAddress.getText().toString());
        startActivity(intent);
    }


}