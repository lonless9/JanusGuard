package com.janusguard.transport.reporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janusguard.agent.AgentConfig;
import com.janusguard.core.event.SecurityEvent;
import com.janusguard.transport.queue.EventQueue;

/**
 * 数据上报器
 * 负责将安全事件数据上报到外部系统或文件
 */
public class DataReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(DataReporter.class);
    
    private final AgentConfig config;
    private final EventQueue eventQueue;
    private final ObjectMapper objectMapper;
    private ExecutorService reporterThread;
    private ScheduledExecutorService flushScheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<SecurityEvent> eventBuffer = new ArrayList<>();
    private PrintWriter fileWriter;
    
    /**
     * 构造函数
     * 
     * @param config Agent配置
     * @param eventQueue 事件队列
     */
    public DataReporter(AgentConfig config, EventQueue eventQueue) {
        this.config = config;
        this.eventQueue = eventQueue;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 启动数据上报器
     */
    public synchronized void start() {
        if (running.get()) {
            logger.warn("Data reporter is already running");
            return;
        }
        
        logger.info("Starting data reporter");
        
        try {
            // 初始化数据上报模式
            String reportingMode = config.getString("reporting.mode", "file");
            
            // 初始化文件写入器（如果需要）
            if ("file".equals(reportingMode)) {
                initFileWriter();
            }
            
            // 创建上报线程
            reporterThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "janusguard-reporter");
                    thread.setDaemon(true);
                    return thread;
                }
            });
            
            // 创建定时刷新调度器
            int flushInterval = config.getInt("event-processing.flush-interval-ms", 5000);
            flushScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "janusguard-flush-scheduler");
                    thread.setDaemon(true);
                    return thread;
                }
            });
            
            // 启动上报线程
            reporterThread.submit(new ReporterWorker());
            
            // 启动定时刷新
            flushScheduler.scheduleAtFixedRate(this::flushBuffer, 
                    flushInterval, flushInterval, TimeUnit.MILLISECONDS);
            
            running.set(true);
            logger.info("Data reporter started with mode: {}", reportingMode);
        } catch (Exception e) {
            logger.error("Failed to start data reporter", e);
            throw new RuntimeException("Data reporter start failed", e);
        }
    }
    
    /**
     * 停止数据上报器
     */
    public synchronized void stop() {
        if (!running.get()) {
            logger.warn("Data reporter is not running");
            return;
        }
        
        logger.info("Stopping data reporter");
        
        running.set(false);
        
        try {
            // 刷新剩余事件
            flushBuffer();
            
            // 关闭调度器
            if (flushScheduler != null) {
                flushScheduler.shutdown();
                try {
                    if (!flushScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        flushScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    flushScheduler.shutdownNow();
                }
            }
            
            // 关闭上报线程
            if (reporterThread != null) {
                reporterThread.shutdown();
                try {
                    if (!reporterThread.awaitTermination(5, TimeUnit.SECONDS)) {
                        reporterThread.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reporterThread.shutdownNow();
                }
            }
            
            // 关闭文件写入器
            closeFileWriter();
            
            logger.info("Data reporter stopped");
        } catch (Exception e) {
            logger.error("Error stopping data reporter", e);
            throw new RuntimeException("Data reporter stop failed", e);
        }
    }
    
    /**
     * 初始化文件写入器
     */
    private void initFileWriter() throws IOException {
        String logPath = config.getString("reporting.file.path", "./logs/janusguard-events.log");
        
        // 确保目录存在
        Path logDir = Paths.get(logPath).getParent();
        if (logDir != null) {
            Files.createDirectories(logDir);
        }
        
        // 创建文件写入器
        fileWriter = new PrintWriter(new FileWriter(logPath, true));
        logger.info("File writer initialized for path: {}", logPath);
    }
    
    /**
     * 关闭文件写入器
     */
    private void closeFileWriter() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
                logger.info("File writer closed");
            } catch (Exception e) {
                logger.error("Error closing file writer", e);
            }
        }
    }
    
    /**
     * 刷新缓冲区中的事件
     */
    private synchronized void flushBuffer() {
        if (eventBuffer.isEmpty()) {
            return;
        }
        
        String reportingMode = config.getString("reporting.mode", "file");
        
        try {
            if ("file".equals(reportingMode)) {
                // 将事件写入文件
                for (SecurityEvent event : eventBuffer) {
                    if (fileWriter != null) {
                        fileWriter.println(objectMapper.writeValueAsString(event));
                    }
                }
                
                if (fileWriter != null) {
                    fileWriter.flush();
                }
            } else if ("http".equals(reportingMode)) {
                // 将来会实现HTTP上报
                logger.info("HTTP reporting not implemented yet, events will be logged only");
                for (SecurityEvent event : eventBuffer) {
                    logger.info("Event for HTTP reporting: {}", event);
                }
            } else if ("grpc".equals(reportingMode)) {
                // 将来会实现gRPC上报
                logger.info("gRPC reporting not implemented yet, events will be logged only");
                for (SecurityEvent event : eventBuffer) {
                    logger.info("Event for gRPC reporting: {}", event);
                }
            }
            
            logger.debug("Flushed {} events", eventBuffer.size());
            eventBuffer.clear();
        } catch (Exception e) {
            logger.error("Error flushing event buffer", e);
        }
    }
    
    /**
     * 上报工作线程
     */
    private class ReporterWorker implements Runnable {
        @Override
        public void run() {
            logger.info("Reporter worker thread started");
            
            int batchSize = config.getInt("event-processing.batch-size", 100);
            
            while (running.get() || eventQueue.getProcessedQueueSize() > 0) {
                try {
                    // 从队列获取已处理事件
                    SecurityEvent event = eventQueue.pollProcessed();
                    
                    if (event != null) {
                        synchronized (DataReporter.this) {
                            eventBuffer.add(event);
                            
                            // 达到批处理大小时刷新
                            if (eventBuffer.size() >= batchSize) {
                                flushBuffer();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("Reporter worker thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in reporter worker", e);
                }
            }
            
            // 最后一次刷新
            flushBuffer();
            
            logger.info("Reporter worker thread stopped");
        }
    }
} 