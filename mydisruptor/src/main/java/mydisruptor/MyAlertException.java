package mydisruptor;

/**
 * 被唤醒异常（用于消费者优雅停止）
 * */
public class MyAlertException extends Exception{

    /**
     * 单例异常
     * */
    public static final MyAlertException INSTANCE = new MyAlertException();
}
