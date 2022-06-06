package mydisruptor.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

/**
 * 启发性的查询是否存在Thread.onSpinWait方法，如果有则可以调用，如果没有则执行空逻辑
 *
 * 兼容老版本无该方法的jdk（Thread.onSpinWait是jdk9开始引入的）
 * */
public class MyThreadHints {

    private static final MethodHandle ON_SPIN_WAIT_METHOD_HANDLE;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle methodHandle = null;
        try {
            methodHandle = lookup.findStatic(Thread.class, "onSpinWait", methodType(void.class));
        } catch (final Exception ignore) {
            // jdk9才引入的Thread.onSpinWait, 低版本没找到该方法直接忽略异常即可
        }

        ON_SPIN_WAIT_METHOD_HANDLE = methodHandle;
    }

    public static void onSpinWait() {
        // Call java.lang.Thread.onSpinWait() on Java SE versions that support it. Do nothing otherwise.
        // This should optimize away to either nothing or to an inlining of java.lang.Thread.onSpinWait()
        if (null != ON_SPIN_WAIT_METHOD_HANDLE) {
            try {
                // 如果是高版本jdk找到了Thread.onSpinWait方法，则进行调用, 插入特殊指令优化CPU自旋性能（例如x86架构中的pause汇编指令）
                // invokeExact比起反射调用方法要高一些，详细的原因待研究
                ON_SPIN_WAIT_METHOD_HANDLE.invokeExact();
            }
            catch (final Throwable ignore) {
                // 异常无需考虑
            }
        }
    }
}
