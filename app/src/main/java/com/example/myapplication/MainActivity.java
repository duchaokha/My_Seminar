package com.example.myapplication;

import static java.lang.Integer.*;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;
public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user = auth.getCurrentUser();

    // Define variables
    boolean Is_Light_On = true;
    public String Topic = String.valueOf(user.getEmail());
    public String subMess = "";
    public int statusDevice;

    // Create random client ID
    Random rand = new Random();
    int upperbound = 100000;
    int int_random = rand.nextInt(upperbound);
    String clientID = "client_random_ID" + int_random;
    // Create mqtt client
    Mqtt3AsyncClient client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientID)
            .serverHost("d861533d074544ffb95af20146317ee4.s2.eu.hivemq.cloud")
            .serverPort(8883)
            .useSslWithDefaultConfig()
            .buildAsync();


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect mqtt broker
        connect(client);

        // Subscribe
        subscribe(client, Topic);

        check_Status();
        set_Status();
    }

    public void logout(View v)
    {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }

    // Check device status
    public void check_Status()
    {
        if (subMess.isEmpty()) {
            statusDevice = 2;
        } else {
            statusDevice = Integer.parseInt(subMess);
        }
    }

    // Set device status
    public void set_Status()
    {
        Button Status = findViewById(R.id.button);
        switch(statusDevice) {
            case 0:
                Is_Light_On = false;
                Status.setText("Light is off");
                break;
            case 1:
                Is_Light_On = true;
                Status.setText("Light is on");
                break;
            case 2:
                Status.setText("Power's down!");
                break;

        }
    }

    // Toggle light
    public void toggle(View v)
    {
        if( statusDevice == 0 || statusDevice == 1 ) {
            Is_Light_On = !Is_Light_On;
            if (Is_Light_On) {
                publish(client, Topic, "1");
            } else {
                publish(client, Topic, "0");
            }
        }
    }

    // Connect to broker
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void connect(Mqtt3AsyncClient client) {
        client.connectWith()
                .simpleAuth()
                .username("android")
                .password("MySeminar".getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        Log.e("mqtt:", "There's some error when connect");
                    } else {
                        Log.e("mqtt:", "Connect oke hihi");
                    }
                });
    }

    // Publish to a topic
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void publish(Mqtt3AsyncClient client, String topicName, String mess) {
        client.publishWith()
                .topic(topicName)
                .qos(MqttQos.AT_LEAST_ONCE)
                .retain(true)
                .payload(mess.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e("mqtt:", "There's some error when publish");
                    }
                    else {
                        Log.e("mqtt:", "Publish oke r nha");
                    }
                });
    }

    // Subscribe to a topic
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void subscribe(Mqtt3AsyncClient client, String topicName)
    {
        client.subscribeWith()
                .topicFilter(topicName)
                .callback(publish -> {
                    subMess = new String(publish.getPayloadAsBytes());
                    Log.e("mqtt:", subMess);
                    check_Status();
                    set_Status();

                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e("mqtt:", "There's some error when subscribe");
                    }
                    else {
                        Log.e("mqtt:", "Subscribe oke roi nha");
                    }
                });

    }

}