package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity {

    //============================ Sensor ============================
    private Spinner pwmSelect; //PWM下拉式選單
    private TextView tempText, humiText, airText, sunText; //顯示感測器數值的文字區
    private TextView tempHigh, tempLow, humiHigh, humiLow, airHigh, airLow;

    private String tempResult, humiResult, airResult, sunResult,nowPwm1;
    int airValue, tempValue, humiValue, lightValue;
    int nowPwm = 64;
    int percentage = 50;

    //============================ PWM ============================
    private String pwmUrl; //設定PWM時所需要的URL
    Http_Get HG; //建立Http_Get物件

    //============================ Notifications ============================
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TEST_NOTIFY_ID = "test_notyfy_id";
    private static final int NOTYFI_REQUEST_ID = 300;
    private int testNotifyId = 11;
    int tempCount, humiCount, airCount;

    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HG = new Http_Get();

        tempText = findViewById(R.id.tempText);
        humiText = findViewById(R.id.humiText);
        airText = findViewById(R.id.airText);
        sunText = findViewById(R.id.sunText);
        pwmSelect = findViewById(R.id.pwmSelect);

        tempHigh = findViewById(R.id.tempHigh);
        tempLow = findViewById(R.id.tempLow);
        humiHigh = findViewById(R.id.humiHigh);
        humiLow = findViewById(R.id.humiLow);
        airHigh = findViewById(R.id.airHigh);
        airLow = findViewById(R.id.airLow);

        //調整電燈亮度
        pwmSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                String value = parent.getSelectedItem().toString();

                if (value.equals("關")){
                    pwmUrl = "http://192.168.43.231/gpio/0";

                }else if (value.equals("開")){
                    pwmUrl = "http://192.168.43.231/gpio/100";
//                    Notifications("temp");

                }else if (value.equals("自動")){
                    final Runnable returnauto = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                while (lightValue>0&&lightValue<100) {
                                    if (lightValue < 10) {
                                        if (nowPwm > 0) {
                                            percentage = percentage + 5;
                                            nowPwm = percentage;
                                        }
                                        nowPwm1 = String.valueOf(nowPwm);
                                        pwmUrl = "http://192.168.43.231/gpio/";
                                        pwmUrl += nowPwm1;
                                        HG.Get(pwmUrl);
                                        //break;
                                        Thread.sleep(1000);
                                    }
                                    if (lightValue > 20) {
                                        if (nowPwm < 100) {
                                            percentage = percentage - 5;
                                            nowPwm = percentage;
                                        }
                                        nowPwm1 = String.valueOf(nowPwm);
                                        pwmUrl = "http://192.168.43.231/gpio/";
                                        pwmUrl += nowPwm1;
                                        HG.Get(pwmUrl);
                                        //break;
                                        Thread.sleep(1000);
                                    }

                                    Thread.sleep(1000);
                                }

                            }catch (Exception e) {
                                // 如果出事，回傳錯誤訊息
                                sunResult = e.toString();
                            }

                        }
                    };
                    Thread thread2 = new Thread(returnauto);
                    thread2.start();

                }else {
                    nowPwm = Integer.parseInt(value);

                    pwmUrl = "http://192.168.43.231/gpio/";
                    pwmUrl += value;
                }
                HG.Get(pwmUrl);
            }

            @Override
            public void onNothingSelected(AdapterView parent) {

            }
        });

        Thread thread = new Thread(catchData);

        thread.start(); // 開始執行

    }

    //根據警戒值改變字體顏色 綠色:安全 | 紅色:危險
    //並且在超過警戒值時跳出通知
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void Notifications(String sensor){

        int  max = 0, min = 0;
        int sensorValue = 25;
        boolean maxStatus = false;
        boolean minStatus = false;

        //============================ temp ============================
        if (sensor.equals("temp")) {

            tempCount++;

            if (!tempHigh.getText().toString().matches("")) {
                String str = tempHigh.getText().toString();
                max = Integer.parseInt(str);
                maxStatus = true;
            }

            if (!tempLow.getText().toString().matches("")) {
                String str = tempLow.getText().toString();
                min = Integer.parseInt(str);
                minStatus = true;
            }

            //只有設定最高警戒值
            if (maxStatus && !minStatus){
                if (tempValue > max) {
                    tempText.setTextColor(Color.parseColor("#ff0000"));

                    if (tempCount > 10){
                        showNotification("溫度異常", "溫度超過設定警戒值警戒值");
                        tempCount = 0;
                    }

                }else {
                    tempText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //只有設定最低警戒值
            else if(!maxStatus && minStatus){
                if (tempValue < min) {
                    tempText.setTextColor(Color.parseColor("#ff0000"));

                    if (tempCount > 60){
                        showNotification("溫度異常", "溫度超過設定警戒值警戒值");
                        tempCount = 0;
                    }
                }else {
                    tempText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //最高與最低皆有設定
            else if(maxStatus && minStatus){
                if (tempValue > max || tempValue < min) {
                    tempText.setTextColor(Color.parseColor("#ff0000"));

                    if (tempCount > 60){
                        showNotification("溫度異常", "溫度超過設定警戒值警戒值");
                        tempCount = 0;
                    }
                }else {
                    tempText.setTextColor(Color.parseColor("#00ff00"));
                }
            }else{
                tempText.setTextColor(Color.DKGRAY);
            }
        }

        //============================ humi ============================
        else if (sensor.equals("humi")) {

            humiCount++;

            if (!humiHigh.getText().toString().matches("")) {
                String str = humiHigh.getText().toString();
                max = Integer.parseInt(str);
                maxStatus = true;
            }

            if (!humiLow.getText().toString().matches("")) {
                String str = humiLow.getText().toString();
                min = Integer.parseInt(str);
                minStatus = true;
            }

            //只有設定最高警戒值
            if (maxStatus && !minStatus){
                if (humiValue > max) {
                    humiText.setTextColor(Color.parseColor("#ff0000"));

                    if (humiCount > 60){
                        showNotification("濕度異常", "濕度超過設定警戒值警戒值");
                        humiCount = 0;
                    }
                }else {
                    humiText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //只有設定最低警戒值
            else if(!maxStatus && minStatus){
                if (humiValue < min) {
                    humiText.setTextColor(Color.parseColor("#ff0000"));

                    if (humiCount > 60){
                        showNotification("濕度異常", "濕度超過設定警戒值警戒值");
                        humiCount = 0;
                    }
                }else {
                    humiText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //最高與最低皆有設定
            else if(maxStatus && minStatus){
                if (humiValue > max || humiValue < min) {
                    humiText.setTextColor(Color.parseColor("#ff0000"));

                    if (humiCount > 60){
                        showNotification("濕度異常", "濕度超過設定警戒值警戒值");
                        humiCount = 0;
                    }
                }else {
                    humiText.setTextColor(Color.parseColor("#00ff00"));
                }
            }else{
                humiText.setTextColor(Color.DKGRAY);
            }
        }

        //============================ air ============================
        else if (sensor.equals("air")) {

            airCount++;

            if (!airHigh.getText().toString().matches("")) {
                String str = airHigh.getText().toString();
                max = Integer.parseInt(str);
                maxStatus = true;
            }

            if (!airLow.getText().toString().matches("")) {
                String str = airLow.getText().toString();
                min = Integer.parseInt(str);
                minStatus = true;
            }

            //只有設定最高警戒值
            if (maxStatus && !minStatus){
                if (airValue > max) {
                    airText.setTextColor(Color.parseColor("#ff0000"));

                    if (airCount > 60){
                        showNotification("氣體濃度異常", "氣體濃度超過設定警戒值警戒值");
                        airCount = 0;
                    }
                }else {
                    airText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //只有設定最低警戒值
            else if(!maxStatus && minStatus){
                if (airValue < min) {
                    airText.setTextColor(Color.parseColor("#ff0000"));

                    if (airCount > 60){
                        showNotification("氣體濃度異常", "氣體濃度超過設定警戒值警戒值");
                        airCount = 0;
                    }
                }else {
                    airText.setTextColor(Color.parseColor("#00ff00"));
                }
            }

            //最高與最低皆有設定
            else if(maxStatus && minStatus){
                if (airValue > max || airValue < min) {
                    airText.setTextColor(Color.parseColor("#ff0000"));

                    if (airCount > 60){
                        showNotification("氣體濃度異常", "氣體濃度超過設定警戒值警戒值");
                        airCount = 0;
                    }
                }else {
                    airText.setTextColor(Color.parseColor("#00ff00"));
                }
            }else{
                airText.setTextColor(Color.DKGRAY);
            }
        }

    }


    //======================取得資料庫感測器資料======================
    private final Runnable catchData = new Runnable() {
        @SuppressLint("SetTextI18n")
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void run() {

            try {
                // 建立取得回應的物件
                while (true) {

                    //==========================light.php==========================
                    // 宣告 HTTP 連線需要的物件
                    URL url3 = new URL("http://192.168.43.112/light.php");
                    // 建立 Google 比較挺的 HttpURLConnection 物件
                    HttpURLConnection connection3 = (HttpURLConnection) url3.openConnection();
                    // 設定連線方式為 POST
                    connection3.setRequestMethod("POST");
                    connection3.setDoOutput(true); // 允許輸出
                    connection3.setDoInput(true); // 允許讀入
                    connection3.setUseCaches(false); // 不使用快取
                    connection3.connect(); // 開始連線

                    // 取得輸入串流
                    InputStream inputStream3 = connection3.getInputStream();
                    // 讀取輸入串流的資料
                    BufferedReader bufReader3 = new BufferedReader(new InputStreamReader(inputStream3, "utf-8"), 8);

                    String lightData = ""; // 宣告存放用字串
                    String line3 = null; // 宣告讀取用的字串
                    while((line3 = bufReader3.readLine()) != null) {
                        // 每當讀取出一列，就加到存放字串後面
                        lightData += line3;
                    }

                    inputStream3.close(); // 關閉輸入串流
                    sunResult = lightData;

                    lightValue = Integer.parseInt(sunResult);
//                    lightValue = (lightValue/45)*100;
                    sunText.setText(lightValue + "%");

                    //==========================light.php==========================

                    //==========================temp.php==========================
                    // 宣告 HTTP 連線需要的物件
                    URL url1 = new URL("http://192.168.43.112/temp.php");
                    // 建立 Google 比較挺的 HttpURLConnection 物件
                    HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                    // 設定連線方式為 POST
                    connection1.setRequestMethod("POST");
                    connection1.setDoOutput(true); // 允許輸出
                    connection1.setDoInput(true); // 允許讀入
                    connection1.setUseCaches(false); // 不使用快取
                    connection1.connect(); // 開始連線

                    // 取得輸入串流
                    InputStream inputStream1 = connection1.getInputStream();
                    // 讀取輸入串流的資料
                    BufferedReader bufReader1 = new BufferedReader(new InputStreamReader(inputStream1, "utf-8"), 8);

                    String trmpData = ""; // 宣告存放用字串
                    String line1 = null; // 宣告讀取用的字串
                    while((line1 = bufReader1.readLine()) != null) {
                        // 每當讀取出一列，就加到存放字串後面
                        trmpData += line1;
                    }

                    inputStream1.close(); // 關閉輸入串流
                    tempResult = trmpData;
                    tempText.setText(tempResult + "℃");

                    tempValue = Integer.parseInt(tempResult);
                    Notifications("temp");

                    //==========================temp.php==========================

                    //==========================humi.php==========================
                    // 宣告 HTTP 連線需要的物件
                    URL url2 = new URL("http://192.168.43.112/humi.php");
                    // 建立 Google 比較挺的 HttpURLConnection 物件
                    HttpURLConnection connection2 = (HttpURLConnection) url2.openConnection();
                    // 設定連線方式為 POST
                    connection2.setRequestMethod("POST");
                    connection2.setDoOutput(true); // 允許輸出
                    connection2.setDoInput(true); // 允許讀入
                    connection2.setUseCaches(false); // 不使用快取
                    connection2.connect(); // 開始連線

                    // 取得輸入串流
                    InputStream inputStream2 = connection2.getInputStream();
                    // 讀取輸入串流的資料
                    BufferedReader bufReader2 = new BufferedReader(new InputStreamReader(inputStream2, "utf-8"), 8);

                    String humiData = ""; // 宣告存放用字串
                    String line2 = null; // 宣告讀取用的字串
                    while((line2 = bufReader2.readLine()) != null) {
                        // 每當讀取出一列，就加到存放字串後面
                        humiData += line2;
                    }

                    inputStream2.close(); // 關閉輸入串流
                    humiResult = humiData;
                    humiText.setText(humiResult + "%");

                    humiValue = Integer.parseInt(humiResult);
                    Notifications("humi");
                    //==========================humi.php==========================

                    //==========================air.php==========================
                    // 宣告 HTTP 連線需要的物件
                    URL url = new URL("http://192.168.43.112/air.php");
                    // 建立 Google 比較挺的 HttpURLConnection 物件
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // 設定連線方式為 POST
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true); // 允許輸出
                    connection.setDoInput(true); // 允許讀入
                    connection.setUseCaches(false); // 不使用快取
                    connection.connect(); // 開始連線
                    int responseCode = connection.getResponseCode();

                    // 取得輸入串流
                    InputStream inputStream = connection.getInputStream();
                    // 讀取輸入串流的資料
                    BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);

                    String airData = ""; // 宣告存放用字串
                    String line = null; // 宣告讀取用的字串
                    while((line = bufReader.readLine()) != null) {
                        // 每當讀取出一列，就加到存放字串後面
                        airData += line;
                    }

                    inputStream.close(); // 關閉輸入串流
                    airResult = airData; // 把存放用字串放到全域變數
                    airText.setText(airResult + "ppm");

                    airValue = Integer.parseInt(airResult);
                    Notifications("air");
                    //==========================air.php==========================

                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                // 如果出事，回傳錯誤訊息
                tempResult = e.toString();
                humiResult = e.toString();
                airResult = e.toString();
                sunResult = e.toString();
            }



            // 當這個執行緒完全跑完後執行
            runOnUiThread(new Runnable() {
                public void run() {
                    //sunText.setText(sunValue + "%");
                }
            });

        } // "public void run()" End
    }; // "private final Runnable catchData = new Runnable()" End


    //===================== Notifications =====================
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showNotification(String title, String text) {
        Log.d(TAG, "showNotification: ");

        Intent intent =new Intent(getApplicationContext(),MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                NOTYFI_REQUEST_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent);
        NotificationChannel channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(TEST_NOTIFY_ID
                    , "Notify Test"
                    , NotificationManager.IMPORTANCE_HIGH);
            builder.setChannelId(TEST_NOTIFY_ID);
            manager.createNotificationChannel(channel);
        } else {
            builder.setDefaults(Notification.DEFAULT_ALL)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        manager.notify(testNotifyId,
                builder.build());
    }
}