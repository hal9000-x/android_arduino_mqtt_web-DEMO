package com.mqttled.asifalam.mqtt_led;

import android.content.Context;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;

import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    MqttAndroidClient mqttAndroidClient;
    final String serverUri = "tcp://192.168.1.80:1883"; //MQTT server IP, 1883 is the default port
    final String mosquittoUser = "MQTT_USER";
    final String mosquittoPass = "MQTT_PASSWORD";

    String clientId = "AndroidClient" + System.currentTimeMillis();
    String subscriptionTopic = "led/1";
    String publishTopic = "led/1";

    ImageView light_icon;

    int led_status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        light_icon = findViewById(R.id.light_icon);


        light_icon.setImageResource(R.drawable.light_disconnected);

        light_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(led_status == 0)
                {
                    Toast toast = Toast.makeText(MainActivity.this, "Please connect the LED first.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                else if(led_status == 1)
                {
                    publishMessage("led_on");
                }
                else if(led_status == 2)
                {
                    publishMessage("led_off");
                }
            }
        });

        mqttAndroidClient = new MqttAndroidClient(this, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("RemoteApp:", "The Connection complete.");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("RemoteApp:", "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("RemoteApp: ", new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("RemoteApp:", "DONE");
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(mosquittoUser);
        mqttConnectOptions.setPassword(mosquittoPass.toCharArray());
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                    publishMessage("test");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("RemoteApp:fail", exception.getMessage());

                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }


    public void publishMessage(String msg){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            Log.d("RemoteApp:Published!", "Message Published");
            if(!mqttAndroidClient.isConnected()){
                Log.d("RemoteApp:Fail Publi", mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public void subscribeToTopic(){
        Log.d("RemoteApp:subscribeTop",subscriptionTopic);
        Log.d("RemoteApp:publishTopic",publishTopic);
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 2, this.getApplicationContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("RemoteApp1:success", "Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("RemoteApp1:Failed!", "Failed to subscribe");
                }
            },

            new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String r_message = new String(message.getPayload());
                    Log.d("RemoteApp:1arrived", topic+" - "+r_message);

                    if(r_message.equals("confirmed_on")) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 2;
                                light_icon.setImageResource(R.drawable.light_on);
                            }
                        });
                    }
                    else if(r_message.equals("confirmed_off")){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 1;
                                light_icon.setImageResource(R.drawable.light_off);
                            }
                        });
                    }
                    else if(r_message.equals("connected-on")){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 2;
                                light_icon.setImageResource(R.drawable.light_on);
                            }
                        });
                    }
                    else if(r_message.equals("connected-off")){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 1;
                                light_icon.setImageResource(R.drawable.light_off);
                            }
                        });
                    }
                    else if(r_message.equals("connection_lost")){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 0;
                                light_icon.setImageResource(R.drawable.light_disconnected);
                            }
                        });
                    }
                    else if(r_message.equals("connected")){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                led_status = 1;
                                light_icon.setImageResource(R.drawable.light_off);
                            }
                        });
                    }
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }
}
