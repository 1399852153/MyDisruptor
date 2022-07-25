package mydisruptor.util;

public class LogUtil {

    public static void logWithThreadName(String message){
        // 暂时注释掉，为了和官方disruptor比较性能
//        System.out.println(Thread.currentThread().getName() + " " + message + " ");
    }
}
