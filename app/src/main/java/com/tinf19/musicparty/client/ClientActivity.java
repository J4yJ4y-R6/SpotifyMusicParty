package com.tinf19.musicparty.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.databinding.ActivityClientBinding;

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
        intent.putExtra(Constants.USERNAME, binding.usernameEditText.getText().toString());
        startActivity(intent);
    }


}