package com.janusguard.agent;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.core.event.EventProcessor;
import com.janusguard.transformer.ClassTransformer;
import com.janusguard.transport.queue.EventQueue;
import com.janusguard.transport.reporter.DataReporter;

/**
 * Agent管理器
 * 负责协调Agent各组件的生命周期
 */
public class AgentManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);
    
    private final AgentConfig config;
    private final Instrumentation instrumentation;
    private ClassTransformer classTransformer;
    private EventProcessor eventProcessor;
    private EventQueue eventQueue;
    private DataReporter dataReporter;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 构造函数
     * 
     * @param config Agent配置
     * @param instrumentation Instrumentation实例
     */
    public AgentManager(AgentConfig config, Instrumentation instrumentation) {
        this.config = config;
        this.instrumentation = instrumentation;
    }
    
    /**
     * 启动Agent管理器及其管理的组件
     */
    public synchronized void start() {
        if (running.get()) {
            logger.warn("Agent manager is already running");
            return;
        }
        
        try {
            logger.info("Starting Agent manager");
            
            // 检查Agent是否启用
            if (!config.getBoolean("agent.enabled", true)) {
                logger.info("Agent is disabled in configuration, skipping start");
                return;
            }
            
            // 初始化事件队列
            initEventQueue();
            
            // 初始化事件处理器
            initEventProcessor();
            
            // 初始化数据上报器
            initDataReporter();
            
            // 初始化并注册类转换器
            initClassTransformer();
            
            running.set(true);
            logger.info("Agent manager started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Agent manager", e);
            stop(); // 出错时尝试优雅关闭已启动的组件
            throw new RuntimeException("Agent manager start failed", e);
        }
    }
    
    /**
     * 停止Agent管理器及其管理的组件
     */
    public synchronized void stop() {
        if (!running.get()) {
            logger.warn("Agent manager is not running");
            return;
        }
        
        logger.info("Stopping Agent manager");
        
        try {
            // 停止组件（按照与启动相反的顺序）
            stopClassTransformer();
            stopDataReporter();
            stopEventProcessor();
            stopEventQueue();
            
            running.set(false);
            logger.info("Agent manager stopped successfully");
        } catch (Exception e) {
            logger.error("Error occurred while stopping Agent manager", e);
            throw new RuntimeException("Agent manager stop failed", e);
        }
    }
    
    /**
     * 初始化事件队列
     */
    private void initEventQueue() {
        logger.info("Initializing event queue");
        int queueSize = config.getInt("event-processing.queue-size", 10000);
        eventQueue = new EventQueue(queueSize);
        eventQueue.initialize();
        logger.info("Event queue initialized with size {}", queueSize);
    }
    
    /**
     * 初始化事件处理器
     */
    private void initEventProcessor() {
        logger.info("Initializing event processor");
        eventProcessor = new EventProcessor(config, eventQueue);
        eventProcessor.start();
        logger.info("Event processor started");
    }
    
    /**
     * 初始化数据上报器
     */
    private void initDataReporter() {
        logger.info("Initializing data reporter");
        dataReporter = new DataReporter(config, eventQueue);
        dataReporter.start();
        logger.info("Data reporter started");
    }
    
    /**
     * 初始化并注册类转换器
     */
    private void initClassTransformer() {
        logger.info("Initializing class transformer");
        classTransformer = new ClassTransformer(config, eventProcessor, instrumentation);
        instrumentation.addTransformer(classTransformer, true);
        logger.info("Class transformer registered with instrumentation");
    }
    
    /**
     * 停止并移除类转换器
     */
    private void stopClassTransformer() {
        if (classTransformer != null && instrumentation != null) {
            try {
                logger.info("Removing class transformer");
                instrumentation.removeTransformer(classTransformer);
                logger.info("Class transformer removed");
            } catch (Exception e) {
                logger.error("Error removing class transformer", e);
            }
        }
    }
    
    /**
     * 停止数据上报器
     */
    private void stopDataReporter() {
        if (dataReporter != null) {
            try {
                logger.info("Stopping data reporter");
                dataReporter.stop();
                logger.info("Data reporter stopped");
            } catch (Exception e) {
                logger.error("Error stopping data reporter", e);
            }
        }
    }
    
    /**
     * 停止事件处理器
     */
    private void stopEventProcessor() {
        if (eventProcessor != null) {
            try {
                logger.info("Stopping event processor");
                eventProcessor.stop();
                logger.info("Event processor stopped");
            } catch (Exception e) {
                logger.error("Error stopping event processor", e);
            }
        }
    }
    
    /**
     * 停止事件队列
     */
    private void stopEventQueue() {
        if (eventQueue != null) {
            try {
                logger.info("Shutting down event queue");
                eventQueue.shutdown();
                logger.info("Event queue shut down");
            } catch (Exception e) {
                logger.error("Error shutting down event queue", e);
            }
        }
    }
    
    /**
     * 判断Agent管理器是否正在运行
     * 
     * @return 如果正在运行则返回true
     */
    public boolean isRunning() {
        return running.get();
    }
} 