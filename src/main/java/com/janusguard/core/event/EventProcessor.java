package com.janusguard.core.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.agent.AgentConfig;
import com.janusguard.transport.queue.EventQueue;

/**
 * 事件处理器
 * 负责处理和分析从探针收集的事件
 */
public class EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    
    private final AgentConfig config;
    private final EventQueue eventQueue;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 构造函数
     * 
     * @param config Agent配置
     * @param eventQueue 事件队列
     */
    public EventProcessor(AgentConfig config, EventQueue eventQueue) {
        this.config = config;
        this.eventQueue = eventQueue;
    }
    
    /**
     * 启动事件处理器
     */
    public synchronized void start() {
        if (running.get()) {
            logger.warn("Event processor is already running");
            return;
        }
        
        logger.info("Starting event processor");
        
        // 创建处理线程池
        int processorThreads = config.getInt("event-processing.processor-threads", 2);
        executorService = Executors.newFixedThreadPool(processorThreads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "janusguard-event-processor");
                thread.setDaemon(true);
                return thread;
            }
        });
        
        // 启动事件处理
        for (int i = 0; i < processorThreads; i++) {
            executorService.submit(new EventWorker());
        }
        
        running.set(true);
        logger.info("Event processor started with {} threads", processorThreads);
    }
    
    /**
     * 停止事件处理器
     */
    public synchronized void stop() {
        if (!running.get()) {
            logger.warn("Event processor is not running");
            return;
        }
        
        logger.info("Stopping event processor");
        
        running.set(false);
        
        if (executorService != null) {
            executorService.shutdown();
            logger.info("Event processor executor service shutdown initiated");
        }
        
        logger.info("Event processor stopped");
    }
    
    /**
     * 提交事件进行处理
     * 
     * @param event 要处理的事件
     */
    public void processEvent(SecurityEvent event) {
        if (!running.get()) {
            logger.warn("Event dropped because processor is not running: {}", event);
            return;
        }
        
        try {
            // 进行初步处理
            preProcessEvent(event);
            
            // 将事件放入队列
            eventQueue.offer(event);
        } catch (Exception e) {
            logger.error("Failed to process event: {}", event, e);
        }
    }
    
    /**
     * 事件预处理
     * 
     * @param event 要预处理的事件
     */
    private void preProcessEvent(SecurityEvent event) {
        // 设置处理时间戳
        event.setProcessedTimestamp(System.currentTimeMillis());
        
        // 应用基本规则过滤
        applyRules(event);
    }
    
    /**
     * 应用规则进行事件分析
     * 
     * @param event 要分析的事件
     */
    private void applyRules(SecurityEvent event) {
        // TODO: 在阶段二实现规则引擎
        // 目前只做简单的初步分析
        
        // 为命令执行事件设置高优先级
        if (event.getType() == SecurityEventType.COMMAND_EXECUTION) {
            event.setSeverity(SecurityEventSeverity.HIGH);
        }
        
        // 为文件操作设置中等优先级
        else if (event.getType() == SecurityEventType.FILE_OPERATION) {
            event.setSeverity(SecurityEventSeverity.MEDIUM);
        }
        
        // 为内存木马相关操作设置高优先级
        else if (event.getType() == SecurityEventType.CLASS_LOADING || 
                event.getType() == SecurityEventType.JVM_MEMORY_OPERATION ||
                event.getType() == SecurityEventType.JNI_OPERATION) {
            event.setSeverity(SecurityEventSeverity.HIGH);
            
            // 如果事件中标记了可疑活动，记录告警
            if (event.getData("memoryTrojanSuspicious") != null || 
                event.getData("dangerousOperation") != null ||
                event.getData("suspiciousJNI") != null) {
                logger.warn("检测到可能的内存木马活动: {}", event);
            }
        }
        
        // 为动态代理设置中等优先级
        else if (event.getType() == SecurityEventType.DYNAMIC_PROXY) {
            event.setSeverity(SecurityEventSeverity.MEDIUM);
            
            // 如果检测到可疑代理，提高严重级别
            if (event.getData("suspiciousProxy") != null) {
                event.setSeverity(SecurityEventSeverity.HIGH);
                logger.warn("检测到可疑动态代理: {}", event);
            }
        }
        
        // 为其他类型设置低优先级
        else {
            event.setSeverity(SecurityEventSeverity.LOW);
        }
    }
    
    /**
     * 事件处理工作线程
     */
    private class EventWorker implements Runnable {
        @Override
        public void run() {
            logger.info("Event worker thread started");
            
            while (running.get()) {
                try {
                    // 从队列中获取事件
                    SecurityEvent event = eventQueue.poll();
                    
                    if (event != null) {
                        // 处理事件
                        logger.debug("Processing event: {}", event);
                        
                        // TODO: 在后续阶段实现更复杂的事件分析
                        
                        // 将处理完的事件放回队列，供上报器消费
                        eventQueue.offerProcessed(event);
                    }
                } catch (InterruptedException e) {
                    logger.info("Event worker thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing event", e);
                }
            }
            
            logger.info("Event worker thread stopped");
        }
    }
} 