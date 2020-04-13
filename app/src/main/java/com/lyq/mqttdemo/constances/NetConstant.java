package com.lyq.mqttdemo.constances;

/**
 * @author Liuyq
 * @date 2019/1/28
 */
public class NetConstant {
    /**
     * 设置当前用户私有的MQTT的接入点。例如此处示意使用XXX，实际使用请替换用户自己的接入点。接入点的获取方法是，在控制台申请MQTT实例，每个实例都会分配一个接入点域名。
     */
    public static final String BROKER ="tcp://XXXXXXX:1883";
    /**
     * 设置阿里云的AccessKey，用于鉴权
     */
    public static final String ACESS_KEY ="XXXXXXX";
    /**
     * 设置阿里云的SecretKey，用于鉴权
     */
    public static final String SECRET_KEY ="XXXXXXXXXXXXXXXXX";
    /**
     * 用于鉴权
     */
    public static final String INSTANCE_ID = "XXXXXX";
    /**
     * 发消息使用的一级Topic，需要先在MQ控制台里申请
     */
    public static final String TOPIC ="XXX";
    /**
     * MQTT的ClientID，一般由两部分组成，GroupID@@@DeviceID
     * 其中GroupID在MQ控制台里申请
     * DeviceID由应用方设置，可能是设备编号等，需要唯一，否则服务端拒绝重复的ClientID连接
     */
    public static final String CLIENT_ID ="GID_XXX@@@XXX"; // 不全
}
