package com.janusguard.transport.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.core.event.SecurityEvent;

/**
 * 事件队列
 * 负责缓存事件并在生产者与消费者之间传递
 * 在后续阶段会使用Disruptor替换实现以提高性能
 */
public class EventQueue {
    
    private static final Logger logger = LoggerFactory.getLogger(EventQueue.class);
    
    // 原始事件队列（从探针到处理器）
    private final BlockingQueue<SecurityEvent> rawEventQueue;
    
    // 已处理事件队列（从处理器到上报器）
    private final BlockingQueue<SecurityEvent> processedEventQueue;
    
    // 队列容量
    private final int capacity;
    
    // 默认等待超时（毫秒）
    private static final long DEFAULT_POLL_TIMEOUT = 1000;
    
    /**
     * 构造函数
     * 
     * @param capacity 队列容量
     */
    public EventQueue(int capacity) {
        this.capacity = capacity;
        this.rawEventQueue = new LinkedBlockingQueue<>(capacity);
        this.processedEventQueue = new LinkedBlockingQueue<>(capacity);
    }
    
    /**
     * 初始化队列
     */
    public void initialize() {
        logger.info("Initializing event queue with capacity {}", capacity);
    }
    
    /**
     * 关闭队列
     */
    public void shutdown() {
        logger.info("Shutting down event queue");
        rawEventQueue.clear();
        processedEventQueue.clear();
    }
    
    /**
     * 提交原始事件到队列
     * 
     * @param event 要提交的事件
     * @return 是否成功提交
     */
    public boolean offer(SecurityEvent event) {
        if (event == null) {
            return false;
        }
        
        try {
            boolean result = rawEventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
            if (!result) {
                logger.warn("Failed to offer event to raw queue, queue might be full");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while offering event to raw queue");
            return false;
        }
    }
    
    /**
     * 提交已处理事件到队列
     * 
     * @param event 要提交的已处理事件
     * @return 是否成功提交
     */
    public boolean offerProcessed(SecurityEvent event) {
        if (event == null) {
            return false;
        }
        
        try {
            boolean result = processedEventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
            if (!result) {
                logger.warn("Failed to offer event to processed queue, queue might be full");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while offering event to processed queue");
            return false;
        }
    }
    
    /**
     * 从原始队列获取事件
     * 
     * @return 获取的事件，如果队列为空则可能返回null
     * @throws InterruptedException 如果等待过程中被中断
     */
    public SecurityEvent poll() throws InterruptedException {
        return rawEventQueue.poll(DEFAULT_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 从已处理队列获取事件
     * 
     * @return 获取的已处理事件，如果队列为空则可能返回null
     * @throws InterruptedException 如果等待过程中被中断
     */
    public SecurityEvent pollProcessed() throws InterruptedException {
        return processedEventQueue.poll(DEFAULT_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 获取原始队列大小
     * 
     * @return 原始队列中的事件数量
     */
    public int getRawQueueSize() {
        return rawEventQueue.size();
    }
    
    /**
     * 获取已处理队列大小
     * 
     * @return 已处理队列中的事件数量
     */
    public int getProcessedQueueSize() {
        return processedEventQueue.size();
    }
} 