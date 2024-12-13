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
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    int id = 100;
    TextView tv=findViewById(R.id.textView);
    TextView tv2=findViewById(R.id.textView2);
    Button btn_on = findViewById(R.id.button2);
    Button btn_off = findViewById(R.id.button);
    Boolean isConnection=true;
    String ServerAddress ="http://192.168.43.42:8080";
    String CashCommandPath="CashCommand.txt";
    public static final Logger logger = Logger.getLogger(MainActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            LogManager.getLogManager().readConfiguration(
                    MainActivity.class.getResourceAsStream("com/example/iotapplication/logging.properties"));
        } catch (IOException ignored) { }
        logger.log(Level.INFO,"application start");
    }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_on.setOnClickListener(v -> new OnOffSender().execute("on"));

        btn_off.setOnClickListener(v -> new OnOffSender().execute("off"));

        createNotificationChannel();

    String command=GetCashCommand();
        if (!command.equals("none")) new OnOffSender().execute(command);

        RequestSender RC = new RequestSender();
        RC.execute();
    }
private String GetCashCommand()
{
    FileInputStream fin = null;
    try {
        fin = openFileInput(CashCommandPath);
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
private Boolean SetCashCommand(String command)
{
    FileOutputStream fos = null;
    try {
        fos = openFileOutput(CashCommandPath, MODE_PRIVATE);
        fos.write(command.getBytes());
        fos.close();
        return true;
    }
    catch(IOException ex) {
        logger.log(Level.SEVERE,"write file "+CashCommandPath,ex);
        return false;
    }

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

    @SuppressLint("StaticFieldLeak")
    сlass OnOffSender extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
        logger.log(Level.INFO,"try to send "+strings[0]);
        if (!SetCashCommand(strings[0])) return null;

            if (!isConnection) return null;
            boolean endOperation=false;
            while (!endOperation) {
                try {
                    String command=GetCashCommand();
                    URL url = new URL(ServerAddress);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "text/html");

                    con.setDoOutput(true);
                    DataOutputStream dStream = new DataOutputStream(con.getOutputStream());
                    dStream.writeBytes(command);
                    dStream.flush();
                    dStream.close();

                    ShowConnectionOn();
                    endOperation=true;
                    logger.log(Level.FINE,command+" sent");

                } catch (Exception e) {
                    logger.log(Level.SEVERE,"connection failure",e);
                    ShowConnectionOff();
                    try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException ignored) {}
                }
            }
            return null;
        }



    @SuppressLint("StaticFieldLeak")
    class RequestSender extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... voids)
        {
            while (true) {
                StringBuilder data = new StringBuilder();;
                try {
                    URL url = new URL(ServerAddress);
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
                    logger.log(Level.SEVERE,"connection failure",e);
                }
                publishProgress(data.toString());

                try {TimeUnit.SECONDS.sleep(5);} catch (InterruptedException ignored) {}
            }
        }

        @Override
        protected void onProgressUpdate(String... messages) {
            super.onProgressUpdate(messages[0]);


            if (messages[0].equals("w")) {
                logger.log(Level.INFO,"alarm signal");
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