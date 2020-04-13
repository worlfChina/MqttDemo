package com.lyq.mqttdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.lyq.mqttdemo.constances.NetConstant;
import com.lyq.mqttdemo.utils.MacSignature;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MyMqttService extends Service {

    private static final String TAG = "MqttService";

    private static final String IOT_TOPIC = NetConstant.TOPIC + "/" + "XXX";

    private MqttAndroidClient mqttAndroidClient;

    public MyMqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mqttAndroidClient = new MqttAndroidClient(MyMqttService.this, NetConstant.BROKER, NetConstant.CLIENT_ID);
        mqttAndroidClient.setCallback(new MqttCallback() { //设置监听订阅消息的回调

            @Override
            public void connectionLost(Throwable cause) {
                //连接断开
                Log.d(TAG, "connectionLost: 连接丢失");
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                //订阅的消息送达
                String payload = new String(message.getPayload());
                Log.d(TAG, "Topic: " + topic + " Payload: " + payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //即服务器成功delivery消息
                Log.d(TAG, "deliveryComplete: ");
            }
        });
        /**
         * 计算签名，将签名作为MQTT的password。
         * 签名的计算方法，参考工具类MacSignature，第一个参数是ClientID的前半部分，即GroupID
         * 第二个参数阿里云的SecretKey
         */
        String sign = "";
        try {
            sign = MacSignature.macSignature(NetConstant.CLIENT_ID, NetConstant.SECRET_KEY);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //新建连接设置
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        //断开后，是否自动连接
        mqttConnectOptions.setAutomaticReconnect(true);
        //是否清空客户端的连接记录。若为true，则断开后，broker将自动清除该客户端连接信息
        mqttConnectOptions.setCleanSession(false);
        //设置超时时间，单位为秒
        //mqttConnectOptions.setConnectionTimeout(2);
        //心跳时间，单位为秒。即多长时间确认一次Client端是否在线
        mqttConnectOptions.setKeepAliveInterval(60);
        //允许同时发送几条消息（未收到broker确认信息）
        //mqttConnectOptions.setMaxInflight(10);
        //选择MQTT版本
        mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        mqttConnectOptions.setUserName("Signature|" + NetConstant.ACESS_KEY + "|" + NetConstant.INSTANCE_ID);
        mqttConnectOptions.setPassword(sign.toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{NetConstant.BROKER});


        /**
         * 设置订阅方订阅的Topic集合，此处遵循MQTT的订阅规则，可以是一级Topic，二级Topic，P2P消息请订阅/p2p
         */
//            final String[] topicFilters = new String[]{NetConstant.TOPIC + "/notice/", NetConstant.TOPIC + "/p2p"};
//            final int[] qos = {0, 0};
        try {
            //开始连接
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: Success to connect to 连接成功 " + NetConstant.BROKER);
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    //成功连接以后开始订阅
                    subscribeAllTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //连接失败
                    Log.d(TAG, "onFailure: Failed to connect to 连接失败 " + NetConstant.BROKER);
                    exception.printStackTrace();
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    //订阅所有消息
    private void subscribeAllTopics() {
        subscribeToTopic(IOT_TOPIC);
    }

    /**
     * 订阅消息
     */
    public void subscribeToTopic(String subscriptionTopic) {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: Success to Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure: " + exception.getCause());
                    Log.d(TAG, "onFailure: Failed to subscribe " + exception.getMessage());
                    exception.printStackTrace();
                }
            });
        } catch (MqttException ex) {
            Log.d(TAG, "subscribeToTopic: Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage(String msg) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
//            参数topic：发布消息的主题
//            参数payload：消息的字节数组
//            参数qos：提供消息的服务质量，可传0、1或2
//            参数retained：是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(IOT_TOPIC, message);
            Log.d(TAG, "publishMessage: Message Published: " + msg);
        } catch (MqttException e) {
            Log.d(TAG, "publishMessage: Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mqttAndroidClient != null) {
                //服务退出时client断开连接
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}