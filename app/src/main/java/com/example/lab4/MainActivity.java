package com.example.lab4;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = getBaseContext().openOrCreateDatabase("app.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Songs (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT ," +
                "artist TEXT NOT NULL," +
                "track_title TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");");
    }

    @Override
    protected void onStart() {
        super.onStart();
        new JsonTask(findViewById(R.id.text)).execute();
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        private final TextView v;

        JsonTask(TextView v) {
            this.v = v;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("https://webradio.io/api/radio/pi/current-song");
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)
                }

                return buffer.toString();


            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            try {
                JSONObject j = new JSONObject(result);
                String art = j.getString("artist"), title = j.getString("title");

                long RowsCount = DatabaseUtils.queryNumEntries(db,"Songs");

                if (RowsCount == 0) {
                    db.execSQL("INSERT INTO Songs (artist, track_title) VALUES (?, ?)", new Object[]{art, title});
                } else{
                    db.execSQL("INSERT INTO Songs (artist, track_title) SELECT ?, ? FROM Songs WHERE (artist != ? AND track_title != ? AND created_at = (SELECT MAX(created_at) FROM Songs)) LIMIT 1", new Object[]{art, title, art, title});
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }

            Cursor query = db.rawQuery("SELECT artist, track_title, created_at FROM Songs ORDER BY created_at DESC LIMIT 20;", null);
            StringBuilder r = new StringBuilder("Artist, Track, Created At\n");
            while (query.moveToNext()) {
                r.append(query.getString(0)).append(", ")
                        .append(query.getString(1)).append(", ")
                        .append(query.getString(2)).append("\n");
            }
            query.close();

            v.setText(r.toString());

            try {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        new JsonTask(findViewById(R.id.text)).execute();
                    }
                }, 20000);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}