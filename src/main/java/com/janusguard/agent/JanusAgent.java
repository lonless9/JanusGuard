package com.janusguard.agent;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JanusGuard Agent主入口类
 * 负责Agent的初始化和生命周期管理
 */
public class JanusAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(JanusAgent.class);
    private static AgentManager agentManager;
    
    /**
     * JVM启动时加载Agent的入口点
     *
     * @param agentArgs 命令行参数
     * @param inst Instrumentation实例
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        logger.info("JanusGuard Agent starting in premain mode");
        try {
            initialize(agentArgs, inst);
            logger.info("JanusGuard Agent started successfully in premain mode");
        } catch (Exception e) {
            logger.error("Failed to start JanusGuard Agent in premain mode", e);
        }
    }
    
    /**
     * 运行时动态附加Agent的入口点
     *
     * @param agentArgs 命令行参数
     * @param inst Instrumentation实例
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        logger.info("JanusGuard Agent starting in agentmain mode");
        try {
            initialize(agentArgs, inst);
            logger.info("JanusGuard Agent started successfully in agentmain mode");
        } catch (Exception e) {
            logger.error("Failed to start JanusGuard Agent in agentmain mode", e);
        }
    }
    
    /**
     * 初始化Agent
     *
     * @param agentArgs 命令行参数
     * @param inst Instrumentation实例
     */
    private static synchronized void initialize(String agentArgs, Instrumentation inst) {
        if (agentManager != null) {
            logger.warn("JanusGuard Agent is already initialized");
            return;
        }
        
        // 初始化配置
        AgentConfig config = new AgentConfig();
        config.initialize(agentArgs);
        
        // 创建并启动Agent管理器
        agentManager = new AgentManager(config, inst);
        agentManager.start();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM is shutting down, stopping JanusGuard Agent");
            shutdown();
        }));
    }
    
    /**
     * 关闭Agent
     */
    public static synchronized void shutdown() {
        if (agentManager != null) {
            try {
                agentManager.stop();
                logger.info("JanusGuard Agent stopped successfully");
            } catch (Exception e) {
                logger.error("Error stopping JanusGuard Agent", e);
            } finally {
                agentManager = null;
            }
        }
    }
} 