package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class Http_Get extends Service {

    private String getUrl;

    public void Get(String url){
        this.getUrl = url;

        new Thread(new Runnable() {

            @Override
            public void run() {

                //建立HttpClient物件
                HttpClient httpClient = new DefaultHttpClient();
                //建立Http Get，並給予要連線的Url
                HttpGet get = new HttpGet(getUrl);

                //透過Get跟Http Server連線並取回傳值
                try {
                    HttpResponse response = httpClient.execute(get);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

