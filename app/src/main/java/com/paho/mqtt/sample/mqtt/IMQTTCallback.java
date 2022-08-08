package com.paho.mqtt.sample.mqtt;

public interface IMQTTCallback {
    /**
     * 连接成功
     */
    void onConnectSuccess(String msg);

    /**
     * 连接失败
     */
    void onConnectFailure(String msg);

    /**
     * 订阅成功
     */
    void onSubscribeSuccess(String topic);

    /**
     * 订阅失败
     */
    void onSubscribeFailure(String topic,String msg);

    /**
     * 取消订阅成功
     */
    void onUnSubscribeSuccess(String topic);

    /**
     * 取消订阅失败
     */
    void onUnSubscribeFailure(String topic,String msg);

    /**
     * 发送成功
     */
    void onSendSuccess();

    /**
     * 发送失败
     */
    void onSendFailure(String msg);

    /**
     * 收到消息
     */
    void onReceiveMessage(String topic,String payLoad,boolean retain);
}
