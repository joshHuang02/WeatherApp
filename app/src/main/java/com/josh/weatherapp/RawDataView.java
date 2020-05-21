package com.josh.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

public class RawDataView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_data_view);

        // Read the saved rawData string from main activity
        FileInputStream fin = null;
        String rawWeatherData="";
        try {
            fin = openFileInput("rawWeatherData");
            int c;
            while( (c = fin.read()) != -1){
                rawWeatherData = rawWeatherData + Character.toString((char)c);
            }
            fin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //display raw json string
        try {
            JSONObject jsonObj = new JSONObject(rawWeatherData);
            TextView dataTxt = (TextView) findViewById(R.id.dataTxt);

            // make the text view scroll-able and format the json string
            dataTxt.setMovementMethod(new ScrollingMovementMethod());
            dataTxt.setText(jsonObj.toString(6));
        } catch (JSONException e) {
            System.out.println(e);
        }
    }

    public void back(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
