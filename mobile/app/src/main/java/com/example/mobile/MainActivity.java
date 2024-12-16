package com.example.mobile;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener  {
    int id = 100;
    TextView tv;
    TextView tv2;
    TextView tv_token;
    Button btn_on;
    Button btn_off;
    Button btn_qr;
    String CommandPath ="CashCommand.txt";
    String TokenPath="Token.txt";
    Logger logger;
    String ServerAddress ="http://192.168.1.100:8080";  // поправить
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv=findViewById(R.id.textView);
        tv2=findViewById(R.id.textView2);
        tv_token=findViewById(R.id.textView3);
        btn_on = findViewById(R.id.button2);
        btn_off = findViewById(R.id.button);
        btn_qr = findViewById(R.id.button3);

        String config="handlers= java.util.logging.ConsoleHandler,java.util.logging.FileHandler\n" +
                "\n" +
                "java.util.logging.FileHandler.pattern = /storage/emulated/0/IoTLog/IotAppLog%u.txt\n" +
                "java.util.logging.FileHandler.level = ALL\n" +
                "java.util.logging.FileHandler.limit = 1000000\n" +
                "java.util.logging.FileHandler.count = 10\n" +
                "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
                "\n" +
                "java.util.logging.ConsoleHandler.level = ALL\n" +
                "java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter";
        FileOutputStream fos = null;
        try {
            fos = openFileOutput("logging.properties",MODE_PRIVATE);
            fos.write(config.getBytes());
            fos.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE,"properties write fatal",e);
        }


        logger = Logger.getLogger(MainActivity.class.getName());

        try {
            FileInputStream fis = openFileInput("logging.properties");
            LogManager.getLogManager().readConfiguration(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE,"properties is not found",e);
        }

        /*
        try {
            System.setProperty("java.util.logging.config.file",
                    "logging.properties");
            logger.log(Level.INFO,"logger is ready");
        } catch (Exception e) {
            logger.log(Level.SEVERE,"logger is not ready",e);
        }

         */

        logger.log(Level.INFO,"application start");

        btn_on.setOnClickListener(v -> SetStringToFile("on", CommandPath));
        btn_off.setOnClickListener(v -> SetStringToFile("off", CommandPath));
        btn_qr.setOnClickListener(this);

        createNotificationChannel();

        token=GetStringFromFile(TokenPath);
        tv_token.setText(token);

        ShowConnectionOff();

        RequestSender RC = new RequestSender();
        RC.execute();
    }
    @Override
    public void onClick(View v) {
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setPrompt("Scan a barcode or QR Code");
        intentIntegrator.setOrientationLocked(true);
        intentIntegrator.initiateScan();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                //Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                token=intentResult.getContents();
                tv_token.setText(token);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String GetStringFromFile(String path)
    {
    try {
        FileInputStream fin = openFileInput(path);
        byte[] bytes = new byte[fin.available()];
        fin.read(bytes);
        fin.close();
        return new String (bytes);
    }
    catch(FileNotFoundException ex) {
        SetStringToFile("none",path);
    }
    catch(IOException ex) {
        logger.log(Level.SEVERE,"read file "+path,ex);
    }
    return "none";
}
    private void SetStringToFile(String data,String path)
    {
    try {
        FileOutputStream fos = openFileOutput(path,MODE_PRIVATE);
        fos.write(data.getBytes());
        fos.close();
    }
    catch(IOException ex) {
        logger.log(Level.SEVERE,"write file "+path,ex);
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
                String command=GetStringFromFile(CommandPath);
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
                            con.setConnectTimeout(2000);
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
                            SetStringToFile("none", CommandPath);
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

                try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException ignored) {}
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