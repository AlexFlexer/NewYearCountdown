package com.alexflex.experiments.untilnewyear;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ConstraintLayout container;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private boolean serviceIsStarted;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Button addPhoto;
    private RadioButton custom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        container = findViewById(R.id.container);
        sharedPreferences = getPreferences(MODE_PRIVATE);
        editor = sharedPreferences.edit();
        serviceIsStarted = sharedPreferences.getBoolean("isServiceStarted", false);
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_design:
                        setStyleSelectLayout();
                        return true;
                    case R.id.navigation_settings:
                        setSettingsLayout();
                        return true;
                }
                return false;
            }
        });
        setStyleSelectLayout();
        addPhoto = findViewById(R.id.add_photo);
        addPhoto.setOnClickListener(this);

        //runtime request на запись в памяти
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    public void setStyleSelectLayout(){
        container.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.layout_design, container, true);
        ToggleButton toggleButton = findViewById(R.id.toggleButton);
        if(serviceIsStarted){
            toggleButton.setChecked(true);
        } else {
            toggleButton.setChecked(false);
        }
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                editor.putBoolean("isServiceStarted", isChecked);
                editor.commit();
                if (isChecked) {
                    //стартуем сервис
                    if (!serviceIsStarted)
                        startService(new Intent(MainActivity.this, TimeBeforeNY.class));
                    serviceIsStarted = true;
                    editor.putBoolean("isServiceStarted", true);
                } else {
                    //стопаем сервис
                    if (serviceIsStarted)
                        stopService(new Intent(MainActivity.this, TimeBeforeNY.class));
                    serviceIsStarted = false;
                    editor.putBoolean("isServiceStarted", false);
                }
            }
        });

        RadioGroup radioGroup = findViewById(R.id.images);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                editor.putInt("selectedImage", i);
                editor.commit();
            }
        });
        toggleButton.setChecked(sharedPreferences.getBoolean("isServiceStarted", false));
    }

    public void setSettingsLayout(){
        container.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.layout_settings, container, true);
        Spinner spinner = findViewById(R.id.intervals);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.putInt("interval", i);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner.setSelection(sharedPreferences.getInt("interval", 1));
    }

    @Override
    public void onClick(View v) {

        //обработка нажатий на кнопку добавления своих фото
        if (v.getId() == R.id.add_photo) {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(i, 5);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 5) {
            if (resultCode == RESULT_OK) {

                Uri uri = data.getData();
                if(uri != null) {
                    //фото успешно выбрано

                    if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        //память недоступна
                        Toast.makeText(this, R.string.storage_not_accessable, Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        //всё ок

                        //создаём папку и файл в ней
                        String folderName = "Wonderful Timer";
                        File folder = new File(Environment.getExternalStorageDirectory(), folderName);
                        if(!folder.exists()) {
                            folder.mkdir();
                        }
                        try {
                            //создаём файл
                            File photo = new File(folder, System.currentTimeMillis() + ".jpg");
                            photo.createNewFile();

                            //копируем выбранное фото в файл в нашем приложении
                            inputStream = getContentResolver().openInputStream(uri);
                            outputStream = new FileOutputStream(photo);
                            byte[] buffer = new byte[1024];
                            int len;
                            while((len = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                            }

                            editor.putString("customPhotoPath", photo.getPath());
                            editor.commit();
                            Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
                        } catch (FileNotFoundException e) {
                            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        } catch (IOException e) {
                            Toast.makeText(this, R.string.IO_Fail, Toast.LENGTH_LONG).show();
                        } finally {
                            try {
                                if(outputStream != null)
                                    outputStream.close();
                                if(inputStream != null)
                                    inputStream.close();
                            } catch (IOException ignored) { }
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.nothing_selected, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (resultCode == RESULT_CANCELED) {
                //выбор отменён
                Toast.makeText(this, R.string.pick_cancelled, Toast.LENGTH_SHORT).show();
            } else {
                //выбора нет, ибо кто-то накосячил
                Toast.makeText(this, R.string.trouble, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
