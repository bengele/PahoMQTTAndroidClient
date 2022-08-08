package com.paho.mqtt.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.paho.mqtt.sample.mqtt.IMQTTCallback;
import com.paho.mqtt.sample.mqtt.MQTTClient;

public class DemoActivity extends AppCompatActivity implements IMQTTCallback {
    MQTTClient mClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 创建客户端
         * 客户端实现了自动重连
         * 默认为心跳为5分钟
         */
        mClient = new MQTTClient(this, this);
        mClient.build("host", "clientId");
        //mClient.build("host", "clientId",10*60);
        //配置心跳包
        //mClient.configKeepAlive(10 * 60);
    }

    @Override
    public void onConnectSuccess(String msg) {
        //连接成功，可以执行订阅
    }

    @Override
    public void onConnectFailure(String msg) {
        //连接失败
    }

    @Override
    public void onSubscribeSuccess(String topic) {

    }

    @Override
    public void onSubscribeFailure(String topic, String msg) {

    }

    @Override
    public void onUnSubscribeSuccess(String topic) {

    }

    @Override
    public void onUnSubscribeFailure(String topic, String msg) {

    }

    @Override
    public void onSendSuccess() {

    }

    @Override
    public void onSendFailure(String msg) {

    }

    @Override
    public void onReceiveMessage(String topic, String payLoad, boolean retain) {

    }
}
