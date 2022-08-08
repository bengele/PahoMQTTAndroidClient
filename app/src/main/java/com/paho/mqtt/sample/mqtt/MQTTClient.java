package com.paho.mqtt.sample.mqtt;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.NetworkUtils;
import com.ntt.core.nlogger.NLogger;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLSocketFactory;

public class MQTTClient implements MqttCallback {
    private static final String TAG = MQTTClient.class.getSimpleName();
    private Context mContext;
    private IMQTTCallback mCallback;

    private MqttAndroidClient mqttAndroidClient; //异步Mqtt客户端,
    private MqttConnectOptions mqttConnectOptions;

    private boolean connecting;
    private Timer mReconnectTimer;
    private TimerTask mReconnectTask;

    private final int NORMAL_ALIVE = 5 * 60;

    public MQTTClient(Context context, IMQTTCallback callback) {
        this.mCallback = callback;
        this.mContext = context;
    }

    public MQTTClient build(String host, String clientId) {
        return build(host, clientId, NORMAL_ALIVE);
    }

    public MQTTClient build(String host, String clientId, int aliveInterval) {
        releaseSelf();
        mqttAndroidClient = new MqttAndroidClient(mContext, host, clientId);
        mqttAndroidClient.setCallback(this);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setKeepAliveInterval(aliveInterval);
        //        mqttConnectOptions.setAutomaticReconnect(true);
        SSLSocketFactory sslSocketFactory = MQTTSSLSocketFactory.getSSLSocketFactory();
        mqttConnectOptions.setSocketFactory(sslSocketFactory);
        return this;
    }

    /**
     * 配置心跳包
     * 单位秒
     *
     * @param interval
     */
    public void configKeepAlive(int interval) {
        if (mqttConnectOptions == null) {
            mqttConnectOptions = new MqttConnectOptions();
        }
        mqttConnectOptions.setKeepAliveInterval(interval == 0 ? NORMAL_ALIVE : interval);
        //        startReconnectTask(true);
        disconnect();
    }

    /**
     * 连接
     */
    public void connection() {
        //网络判定要新增PING
        if (!NetworkUtils.isConnected()) {
            emitConnectResult(false, "网络未连接，MQTT无法连接,停止重连任务");
            stopReconnectTask();
            return;
        }
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            emitConnectResult(false, "MQTT已是连接状态");
            return;
        }
        if (connecting) {
            NLogger.d(TAG, "正在连接中...禁止重复连接,等待结果回调");
            return;
        }
        try {
            connecting = true;
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    connecting = false;
                    emitConnectResult(true, "连接状态：" + mqttAndroidClient.isConnected());
                    stopReconnectTask();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    connecting = false;
                    emitConnectResult(false, "MQTT连接失败，" + (e != null ? e.getMessage() : ""));
                    boolean wifi = NetworkUtils.isWifiConnected();
                    boolean mobile = NetworkUtils.isMobileData();
                    boolean ping = (mobile && NetworkUtils.isAvailableByPing("www.qq.com")) || wifi;
                    emitConnectResult(false, "MQTT连接失败，" + (e != null ? e.getMessage() : "") + "网络能否ping通 = " + ping);
                    if (!ping) {
                        NLogger.d(TAG, "网络无法PING通，停止重连");
                        stopReconnectTask();
                        return;
                    }
                    //启动重连任务
                    startReconnectTask(false);
                }
            });
        } catch (Exception e) {
            emitConnectResult(false, e.getMessage());
            connecting = false;
            e.printStackTrace();
            NLogger.w(TAG, "MQTT连接异常", e.getMessage());
        }

    }

    /**
     * 订阅
     */
    public void subscribeTopic(String[] topic, int[] qos) {
        if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
            emitSubscriptResult(false, topic, "MQTT未连接");
            return;
        }
        try {
            mqttAndroidClient.subscribe(topic, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    emitSubscriptResult(true, topic, "订阅成功");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    emitSubscriptResult(false, topic, "订阅失败，" + (e != null ? e.getMessage() : ""));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            NLogger.w(TAG, "MQTT订阅异常", topic, e.getMessage());
            emitSubscriptResult(false, topic, "订阅" + topic + "异常，" + e.getMessage());
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribeTopic(String[] topic) {
        if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
            emitUnSubscriptResult(false, topic, "MQTT未连接");
            return;
        }
        try {
            mqttAndroidClient.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    emitUnSubscriptResult(true, topic, "订阅成功");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    emitUnSubscriptResult(false, topic, "订阅" + topic + "失败，" + (e != null ? e.getMessage() : ""));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            NLogger.w(TAG, "MQTT取消订阅异常", topic, e.getMessage());
            emitUnSubscriptResult(false, topic, "取消订阅" + topic + "异常，" + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(String topic, String data, int qos, boolean retain) {
        if (mqttAndroidClient == null) return;
        if (!mqttAndroidClient.isConnected()) {
            emitSendMessageResult(false, "MQTT未连接,无法发送信息");
            return;
        }
        try {
            byte[] bytes = data != null ? data.getBytes() : new byte[]{};
            mqttAndroidClient.publish(topic, bytes, qos, retain, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    emitSendMessageResult(true, "发送消息成功,内容： " + data + " topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    emitSendMessageResult(false, "发送消息失败,内容： " + data + " topic: " + topic + " 错误: " + (e != null ? e.getMessage() : ""));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            NLogger.w(TAG, "MQTT发送消息异常", e.getMessage());
        }
    }

    /**
     * 断开连接
     * 主动调用-不重连
     */
    public void disconnect() {
        if (mqttAndroidClient == null) return;
        try {
            mqttAndroidClient.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    NLogger.d(TAG, "MQTT主动断开连接成功");
                    stopReconnectTask();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    NLogger.d(TAG, "MQTT主动断开连接失败," + exception.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            NLogger.w(TAG, "MQTT断开连接异常", e.getMessage());
        }

    }

    /**
     * 释放自己
     */
    public void releaseSelf() {
        //释放重连任务
        stopReconnectTask();
        if (mqttAndroidClient != null) {
            disconnect();
            mqttAndroidClient.close();
        }
    }

    /**
     * 开始重连任务
     */
    private void startReconnectTask(boolean force) {
        NLogger.d(TAG, "MQTT准备开始重连...", force);
        try {
            if (force) {
                connection();
                return;
            }
            if (mReconnectTimer != null) return;
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) return;
            mReconnectTimer = new Timer();
            mReconnectTask = new TimerTask() {
                @Override
                public void run() {
                    NLogger.d(TAG, "MQTT启动重连...");
                    connection();
                }
            };
            //10s后重连
            mReconnectTimer.schedule(mReconnectTask, 0, 10 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止重连任务
     */
    private void stopReconnectTask() {
        if (mReconnectTimer == null && mReconnectTask == null) return;
        NLogger.d(TAG, "停止定时重连任务");
        mReconnectTimer.cancel();
        mReconnectTask.cancel();
        mReconnectTimer.purge();
        mReconnectTimer = null;
        mReconnectTask = null;
    }

    /**
     * @param v
     * @param msg
     */
    private void emitConnectResult(boolean v, String msg) {
        if (mCallback == null) return;
        if (v) {
            mCallback.onConnectSuccess(msg);
        } else {
            mCallback.onConnectFailure(msg);
        }
    }

    /**
     * @param v
     * @param msg
     */
    private void emitSendMessageResult(boolean v, String msg) {
        if (mCallback == null) return;
        if (v) {
            mCallback.onSendSuccess();
        } else {
            mCallback.onSendFailure(msg);
        }
    }

    /**
     * @param v
     * @param msg
     */
    private void emitSubscriptResult(boolean v, String topic, String msg) {
        if (mCallback == null) return;
        if (v) {
            mCallback.onSubscribeSuccess(topic);
        } else {
            mCallback.onSubscribeFailure(topic, msg);
        }
    }

    /**
     * @param v
     * @param topics
     * @param msg
     */
    private void emitSubscriptResult(boolean v, String[] topics, String msg) {
        if (topics.length <= 0) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topics.length; i++) {
            sb.append(topics[i] + ",");
        }
        emitSubscriptResult(v, sb.toString(), msg);
    }

    /**
     * @param v
     * @param msg
     */
    private void emitUnSubscriptResult(boolean v, String topic, String msg) {
        if (mCallback == null) return;
        if (v) {
            mCallback.onUnSubscribeSuccess(topic);
        } else {
            mCallback.onUnSubscribeFailure(topic, msg);
        }
    }

    /**
     * @param v
     * @param topics
     * @param msg
     */
    private void emitUnSubscriptResult(boolean v, String[] topics, String msg) {
        if (topics.length <= 0) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topics.length; i++) {
            sb.append(topics[i] + ",");
        }
        emitUnSubscriptResult(v, sb.toString(), msg);
    }


    @Override
    public void connectionLost(Throwable cause) {
        if (cause instanceof MqttException) {
            MqttException e = (MqttException) cause;
            NLogger.w(TAG, "MQTT失去连接，message: " + e.getMessage() + " code: " + e.getReasonCode() + " 网络连接状况：" + NetworkUtils.isConnected());
        } else {
            NLogger.w(TAG, "MQTT失去连接，网络连接状况：" + NetworkUtils.isConnected());
        }
        //启动重连
        startReconnectTask(false);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (TextUtils.isEmpty(topic) || message == null || TextUtils.isEmpty(message.toString())) return;
        if (mCallback != null) mCallback.onReceiveMessage(topic, message.toString(), message.isRetained());
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

}
