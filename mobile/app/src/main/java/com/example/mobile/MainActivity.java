package com.example.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    int id = 100;
    TextView tv=findViewById(R.id.textView);
    TextView tv2=findViewById(R.id.textView2);
    Button btn_on = findViewById(R.id.button2);
    Button btn_off = findViewById(R.id.button);
    Boolean isConnection=true;
    String ServerAdress="http://192.168.43.42:8080"; ////как пример
    String cashData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_on.setOnClickListener(v -> new OnOffSender().execute("on"));

        btn_off.setOnClickListener(v -> new OnOffSender().execute("off"));

        createNotificationChannel();

        ShowConnectionOn();

        RequestSender RC = new RequestSender();
        RC.execute();
    }
    private void ShowConnectionOff()
    {
        tv2.setText("Подключение отсутствует");
        tv2.setTextColor(Color.RED);
        isConnection=false;
    }
    private void ShowConnectionOn()
    {
        tv2.setText("Подключено");
        tv2.setTextColor(Color.GREEN);
        isConnection=true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "myCname";
            String description = "myCD";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("101", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    сlass OnOffSender extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {

            cashData=strings[0];
            if (!isConnection) return null;

            boolean endOperation=false;
            while (!endOperation) {
                try {
                    URL url = new URL(ServerAdress);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "text/html");

                    con.setDoOutput(true);
                    DataOutputStream dStream = new DataOutputStream(con.getOutputStream());
                    dStream.writeBytes(cashData);
                    dStream.flush();
                    dStream.close();

                    ShowConnectionOn();
                    endOperation=true;


                } catch (Exception e) {
                    ShowConnectionOff();
                    try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException ignored) {}
                }
            }
            return null;
        }



    class RequestSender extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... voids)
        {
            while (true) {
                StringBuilder data = new StringBuilder();;
                try {
                    URL url = new URL(ServerAdress);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "text/html");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        data.append(inputLine)
                    }
                    in.close();
                    ShowConnectionOn();
                } catch (Exception e) {
                    ShowConnectionOn();
                }
                publishProgress(data.toString());

                try {TimeUnit.SECONDS.sleep(5);} catch (InterruptedException ignored) {}
            }
        }

        @Override
        protected void onProgressUpdate(String... messages) {
            super.onProgressUpdate(messages[0]);

            if (messages[0].equals("w")) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "101")
                        .setSmallIcon(R.drawable.signalicon)
                        .setContentTitle("ВНИМАНИЕ")
                        .setContentText("сработала сигнализация")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                id += 1;
                if (id > 200) id = 101;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(id, builder.build());

                Date date = new Date();
                tv.setText(date.toString());
            }
        }
    }
}