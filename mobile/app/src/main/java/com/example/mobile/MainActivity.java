package com.example.mobile;

import android.annotation.SuppressLint;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    int id = 100;
    TextView tv;
    TextView tv2;
    Button btn_on;
    Button btn_off;
    String CashCommandPath="CashCommand.txt";
    Logger logger;
    String ServerAddress ="http://192.168.1.106:8080";   // пример
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv=findViewById(R.id.textView);
        tv2=findViewById(R.id.textView2);
        btn_on = findViewById(R.id.button2);
        btn_off = findViewById(R.id.button);

        logger = Logger.getLogger(MainActivity.class.getName());

        try {
            System.setProperty("java.util.logging.config.file",
                    "logging.properties");
            logger.log(Level.INFO,"logger is ready");
        } catch (Exception e) {
            logger.log(Level.SEVERE,"logger is not ready",e);
        }
        logger.log(Level.INFO,"application start");
    }

        btn_on.setOnClickListener(v -> SetCashCommand("on"));

        btn_off.setOnClickListener(v -> SetCashCommand("on"));

        createNotificationChannel();

        ShowConnectionOff();

        RequestSender RC = new RequestSender();
        RC.execute();
    }

private String GetCashCommand()
{
    try {
        FileInputStream fin = openFileInput(CashCommandPath);
        byte[] bytes = new byte[fin.available()];
        fin.read(bytes);
        fin.close();
        return new String (bytes);
    }
    catch(FileNotFoundException ex) {
        SetCashCommand("none");
    }
    catch(IOException ex) {
        logger.log(Level.SEVERE,"read file "+CashCommandPath,ex);
    }
    return "none";
}
private void SetCashCommand(String command)
{

    try {
        FileOutputStream fos = openFileOutput(CashCommandPath, MODE_PRIVATE);
        fos.write(command.getBytes());
        fos.close();
    }
    catch(IOException ex) {
        logger.log(Level.SEVERE,"write file "+CashCommandPath,ex);
    }
}
    private void ShowConnectionOff()
    {
        tv2.setText("Подключение отсутствует");
        tv2.setTextColor(Color.RED);
    }
    private void ShowConnectionOn()
    {
        tv2.setText("Подключено");
        tv2.setTextColor(Color.GREEN);
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

    @SuppressLint("StaticFieldLeak")
    class RequestSender extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... voids)
        {
            while (true) {
                String command=GetCashCommand();
                if (!command.equals("none"))
                {
                    boolean endOperation=false;
                    while (!endOperation) {
                        try {
                            command=GetCashCommand();
                            logger.log(Level.INFO,"try to send command "+command);

                            URL obj = new URL(ServerAddress);
                            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                            con.setRequestMethod("POST");
                            con.setRequestProperty("Content-Type", "text/html");
                            con.setDoOutput(true);

                            OutputStream os = con.getOutputStream();
                            byte[] input = command.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);

                            int r=con.getResponseCode();
                            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                            String inputLine;
                            StringBuilder data=new StringBuilder();
                            while ((inputLine = in.readLine()) != null)
                            {
                                data.append(inputLine);
                            }
                            in.close();


                            publishProgress("connection on");
                            endOperation=true;

                            logger.log(Level.INFO,"command "+command+" sent, code: "+ r +" data: "+data);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE,"connection failure",e);
                            publishProgress("connection off");
                            try {TimeUnit.SECONDS.sleep(2);} catch (InterruptedException ignored) {}
                        }
                    }
                }

                StringBuilder data = new StringBuilder();;
                try {
                    URL url = new URL(ServerAddress);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(2000);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "text/html");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        data.append(inputLine)
                    }
                    in.close();
                    publishProgress("connection on");
                } catch (Exception e) {
                    logger.log(Level.SEVERE,"connection failure",e);
                    publishProgress("connection off");
                }
                publishProgress(data.toString());

                try {TimeUnit.SECONDS.sleep(5);} catch (InterruptedException ignored) {}
            }
        }

        @Override
        protected void onProgressUpdate(String... messages) {
            super.onProgressUpdate(messages[0]);

            switch (messages[0]) {
                case "w":
                    logger.log(Level.INFO, "alarm signal");
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
                    break;
                case "connection on":
                    ShowConnectionOn();
                    break;
                case "connection off":
                    ShowConnectionOff();
                    break;
            }
        }
    }
}