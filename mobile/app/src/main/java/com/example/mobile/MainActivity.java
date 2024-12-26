package com.example.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import java.io.BufferedInputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Integer notificationId;

    TextView textLastAlarm;
    TextView textConnectionStatus;
    TextView textToken;
    Button buttonOn;
    Button buttonOff;
    Button buttonQR;

    final String LAST_COMMAND_FILE_PATH = "Command.txt";
    final String TOKEN_PATH = "Token.txt";
    final String SERVER_ADDRESS_PATH = "ServerAddress.txt";

    final Integer LOG_FILE_COUNT = 5;
    final Integer LOG_SIZE = 1024*100;
    final Boolean LOG_APPEND = true;
    final String LOG_FILE_NAME_PATTERN = "Iot_App_Log_%g.log";

    final Integer CONNECTION_TIMEOUT = 2000;
    final Integer REQUESTS_TIMEOUT = 2;

    final Integer FIRST_NOTIFICATION_ID = 100;
    final Integer MAX_NOTIFICATION_ID = 200;

    final Integer REQUEST_OK=200;
    enum CODE{
        CONNECTION_ON,
        CONNECTION_OFF,
        ALARM
    }
    Logger logger;
    String ServerAddress; // поправить
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textLastAlarm = findViewById(R.id.textView);
        textConnectionStatus = findViewById(R.id.textView2);
        textToken = findViewById(R.id.textView3);
        buttonOn = findViewById(R.id.button2);
        buttonOff = findViewById(R.id.button);
        buttonQR = findViewById(R.id.button3);

        notificationId = FIRST_NOTIFICATION_ID;

        logger = Logger.getLogger(MainActivity.class.getName());
        logger.log(Level.INFO,"application start");

        try {
            String logFileName = Environment.getExternalStorageDirectory() +
                    File.separator +
                    LOG_FILE_NAME_PATTERN;
            FileHandler logHandler = new FileHandler(logFileName, LOG_SIZE,
                    LOG_FILE_COUNT, LOG_APPEND);
            logHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(logHandler);
            logger.log(Level.INFO, "logger is ready");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "logger is not ready: input/output",e);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "logger is not ready: unexpected",e);
        }


        btn_on.setOnClickListener(v -> SetStringToFile("on", LAST_COMMAND_FILE_PATH));
        btn_off.setOnClickListener(v -> SetStringToFile("off",  LAST_COMMAND_FILE_PATH));
        btn_qr.setOnClickListener(this);

        createNotificationChannel();

        token=GetStringFromFile(TOKEN_PATH);
        serverAddress = getStringFromFile(SERVER_ADDRESS_PATH);
        textToken.setText(token);
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
                logger.log(Level.WARNING, "QR reading - canceled");
            } else {
                String[] ConnectionData=intentResult.getContents().split("-");
                ServerAddress=ConnectionData[0];
                token=ConnectionData[1];
                SetStringToFile(ServerAddress,SERVER_ADDRESS_PATH);
                SetStringToFile(token,TOKEN_PATH);
                textToken.setText(String .format("%s %s",serverAddress,token));
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
        logger.log(Level.SEVERE,String.format("read file %s : input/output", path));
    }
    catch (Exception ex) {
        logger.log(Level.SEVERE, String.format("read file %s : unexpected", path));
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
        logger.log(Level.SEVERE,String.format("write file %s : input/output", path));
    }
    catch (Exception ex) {
        logger.log(Level.SEVERE, String.format("write file %s : unexpected", path));
    }
}
    private void ShowConnectionOff()
    {
        textConnectionStatus.setText("Подключение отсутствует");
        textConnectionStatus.setTextColor(Color.RED);
    }
    private void ShowConnectionOn()
    {
        textConnectionStatus.setText("Подключено");
        textConnectionStatus.setTextColor(Color.GREEN);
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
        SSLSocketFactory getSSL() {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(getResources().openRawResource(R.raw.app));
                Certificate ca = certFactory.generateCertificate(caInput);
                caInput.close();
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tmf.getTrustManagers(), null);
                return context.getSocketFactory();
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, "SSL error: certificate", e);
                return null;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "SSL error: input/output", e);
                return null;
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "SSL error: keystore", e);
                return null;
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "SSL error: algorithm", e);
                return null;
            } catch (KeyManagementException e) {
                logger.log(Level.SEVERE, "SSL error: key management", e);
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "SSL error: unexpected", e);
                return null;
            }

        }

        @SuppressLint("AllowAllHostnameVerifier")
        @Override
        protected Void doInBackground(Void... voids)
        {
            while (true) {
                String command=GetStringFromFile(LAST_COMMAND_FILE_PATH);
                if (!command.equals("none"))
                {
                    boolean endOperation=false;
                    while (!endOperation) {
                        try {
                            command=getStringFromFile(LAST_COMMAND_FILE_PATH);
                            logger.log(Level.INFO,String.format("try to send command %s", command));

                            URL obj = new URL(ServerAddress);
                            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                            con.setHostnameVerifier(new AllowAllHostnameVerifier()); //only for testing app
                            con.setSSLSocketFactory(getSSL());
                            con.setRequestMethod("POST");
                            con.setConnectTimeout(CONNECTION_TIMEOUT);
                            con.setRequestProperty("Content-Type", "text/html");
                            on.setRequestProperty("User-Agent", "mobile");
                            con.setRequestProperty("Token", token);
                            con.setDoOutput(true);

                            OutputStream os = con.getOutputStream();
                            byte[] input = command.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);

                            int code=con.getResponseCode();

                            InputStreamReader isr = new InputStreamReader(con.getInputStream());
                            BufferedReader br = new BufferedReader(isr);
                            String inputLine;
                            StringBuilder data = new StringBuilder();
                            while ((inputLine = br.readLine()) != null)
                            {
                                data.append(inputLine);
                            }
                            br.close();
                            if (code==REQUEST_OK) {

                                publishProgress("CODE.CONNECTION_ON");
                                endOperation = true;
                                SetStringToFile("none", LAST_COMMAND_FILE_PATH);
                                logger.log(Level.INFO, String.format("command %s sent, code: %d",command,code));
                            }
                            else
                            {
                                publishProgress(CODE.CONNECTION_OFF);
                                logger.log(Level.SEVERE,String.format("connection error: code %d",code));
                            }

                        } catch (Exception e) {
                            logger.log(Level.SEVERE,"connection failure",e);
                            publishProgress(CODE.CONNECTION_OFF);
                            try {TimeUnit.SECONDS.sleep(REQUESTS_TIMEOUT);}
                            catch (InterruptedException e2)
                            {
                                logger.log(Level.SEVERE, "timeout: interrupted",e2);
                            }
                            catch (Exception e2) {
                                logger.log(Level.SEVERE, "timeout: unexpected",e2);
                            }
                        }
                    }
                }

                StringBuilder data = new StringBuilder();;
                try {
                    URL url = new URL(ServerAddress);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setHostnameVerifier(new AllowAllHostnameVerifier()); //only for testing app
                    con.setSSLSocketFactory(getSSL());
                    con.setConnectTimeout(CONNECTION_TIMEOUT);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "text/html");
                    con.setRequestProperty("User-Agent", "mobile");
                    con.setRequestProperty("Token", token);
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    InputStreamReader isr = new InputStreamReader(con.getInputStream());
                    BufferedReader br = new BufferedReader(isr);

                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        data.append(inputLine)
                    }
                    Integer code=con.getResponseCode();

                    if (code==REQUEST_OK)
                    {
                        publishProgress(CODE.CONNECTION_ON);
                    }
                    else
                    {
                        publishProgress(CODE.CONNECTION_OFF);
                        logger.log(Level.SEVERE,String.format("connection error: code %d",code));
                    }

                } catch (Exception e) {
                    logger.log(Level.SEVERE,"connection failure");
                    publishProgress(CODE.CONNECTION_OFF);
                }
                if (data.toString().equals("w")) {
                    publishProgress(CODE.ALARM);

                    try {
                        TimeUnit.SECONDS.sleep(REQUESTS_TIMEOUT);
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "timeout: interrupted");
                    }
                    catch (Exception e) {
                        logger.log(Level.SEVERE, "timeout: unexpected");
                    }
                }

            }
        }

        @Override
        protected void onProgressUpdate(CODE... messages) {
            super.onProgressUpdate(messages[0]);

            switch (messages[0]) {
                case CONNECTION_ON:
                    showConnectionOn();
                    break;
                case CONNECTION_OFF:
                    showConnectionOff();
                    break;
                case ALARM:
                    logger.log(Level.INFO, "alarm signal");
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "101")
                            .setSmallIcon(R.drawable.signalicon)
                            .setContentTitle("ВНИМАНИЕ")
                            .setContentText("сработала сигнализация")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    notificationId += 1;
                    if (notificationId > MAX_NOTIFICATION_ID)
                        notificationId = FIRST_NOTIFICATION_ID;

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(notificationId, builder.build());

                    Date date = new Date();
                    textLastAlarm.setText(date.toString());
                    break;
                default:
                    logger.log(Level.WARNING, "server answer: unexpected");
            }
        }
    }
}