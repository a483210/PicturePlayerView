package com.xiuyukeji.scheduler;

/**
 * 调度器工具类
 *
 * @author Created by jz on 2017/4/1 11:35
 */
public class SchedulerUtil {

    private SchedulerUtil() {
    }

    /**
     * 等待线程结束
     *
     * @param thread 线程
     */
    public static void join(Thread thread) {
        if (Thread.currentThread().getId() == thread.getId()) {//如果是同一个线程调用则不等待，防止死循环
            return;
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 等待锁
     *
     * @param lock 锁
     */
    public static void lockWait(Object lock) {
        try {
            lock.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
