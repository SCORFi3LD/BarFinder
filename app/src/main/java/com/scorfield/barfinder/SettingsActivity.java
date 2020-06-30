package com.scorfield.barfinder;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String BAR_SHARED_PREFS = "BarCache";
    private static final String BAR_SEARCH_RANGE = "SearchRange";

    int currentProgress;
    ImageButton btnBack;
    Button btnSave;
    SeekBar seekBar;
    TextView txtProgress;

    SharedPreferences sharedPreferences;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(BAR_SHARED_PREFS, MODE_PRIVATE);

        btnBack = findViewById(R.id.btn_settings_back);
        btnSave = findViewById(R.id.btn_save_settings);
        txtProgress = findViewById(R.id.txt_progress);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setProgress(sharedPreferences.getInt(BAR_SEARCH_RANGE, 1) / 1000);
        txtProgress.setText((sharedPreferences.getInt(BAR_SEARCH_RANGE, 1) / 1000) + "km");

        seekBar.setOnSeekBarChangeListener(this);
        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        currentProgress = progress;
        txtProgress.setText(currentProgress + "km");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnSave) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(BAR_SEARCH_RANGE, currentProgress * 1000);
            editor.apply();
            finish();
        }
    }
}
