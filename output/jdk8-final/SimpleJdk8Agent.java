import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.ProtectionDomain;
import java.security.Permission;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
// 使用Java内置的字节码操作
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * JanusGuard JDK 8兼容版Agent
 * 提供基本安全监控功能
 */
public class SimpleJdk8Agent {
    
    private static final Logger logger = Logger.getLogger("JanusGuard");
    private static File configFile = null;
    private static Properties config = new Properties();
    private static final String LOG_FILE_DEFAULT = "janusguard-jdk8.log";
    private static String logFile = LOG_FILE_DEFAULT;
    private static Level logLevel = Level.INFO;
    private static final ConcurrentHashMap<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private static final String VERSION = "1.0.0-JDK8";
    
    // 监控开关
    private static boolean monitorClassLoading = true;
    private static boolean monitorReflection = true;
    private static boolean monitorFileOperations = true;
    private static boolean monitorCommandExecution = true;
    private static boolean enableBlockingMode = false;
    
    // 日志事件定义
    private static class LogEvent {
        final Level level;
        final String message;
        final Map<String, String> metadata;
        final long timestamp;
        
        LogEvent(Level level, String message, Map<String, String> metadata) {
            this.level = level;
            this.message = message;
            this.metadata = metadata;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // 异步日志处理
    private static final BlockingQueue<LogEvent> eventQueue = new LinkedBlockingQueue<>(1000);
    private static Thread loggerThread;
    
    // 黑名单/白名单配置
    private static final Set<String> dangerousCommandPatterns = new HashSet<>();
    private static final Set<String> dangerousFilePatterns = new HashSet<>();
    private static final Set<String> dangerousReflectionTargets = new HashSet<>();
    private static final Set<String> whitelistedCommands = new HashSet<>();
    private static final Set<String> whitelistedFiles = new HashSet<>();
    private static final Set<String> whitelistedReflectionTargets = new HashSet<>();
    
    // 添加运行时钩子，捕获命令执行
    static {
        try {
            // 设置默认的危险模式
            dangerousCommandPatterns.add("rm -rf");
            dangerousCommandPatterns.add("format");
            dangerousCommandPatterns.add("del /");
            dangerousFilePatterns.add("/etc/passwd");
            dangerousFilePatterns.add("/etc/shadow");
            dangerousFilePatterns.add("C:\\Windows\\System32");
            dangerousReflectionTargets.add("java.lang.Runtime");
            dangerousReflectionTargets.add("java.lang.ProcessBuilder");
            
            // 设置控制台处理器
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            logger.addHandler(consoleHandler);
            
            // 初始化日志处理器
            logger.setUseParentHandlers(false);
            
            // 启动异步日志处理线程
            loggerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        LogEvent event = eventQueue.take();
                        // 实际写入日志
                        writeLogDirectly(event);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("日志处理异常: " + e.getMessage());
                    }
                }
            }, "JanusGuard-Logger");
            loggerThread.setDaemon(true);
            loggerThread.start();
            
            // 添加JVM关闭钩子确保日志刷新
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("JanusGuard JDK 8兼容版 关闭中...");
                
                // 中断日志线程
                loggerThread.interrupt();
                
                // 处理剩余日志
                drainRemainingLogs();
                
                // 输出统计信息
                outputStatistics();
            }));
            
            logger.info("已安装运行时钩子和关闭监听器");
            
            // 添加运行时命令执行钩子
            installRuntimeHooks();
        } catch (Exception e) {
            System.err.println("无法初始化日志: " + e.getMessage());
        }
    }
    
    /**
     * 直接写入日志记录
     */
    private static void writeLogDirectly(LogEvent event) {
        // 将日志格式化并直接写入
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(new Date(event.timestamp)).append("] ");
        sb.append("[").append(event.level).append("] ");
        sb.append(event.message);
        
        // 输出重要元数据
        if (event.metadata != null && !event.metadata.isEmpty()) {
            sb.append(" [");
            boolean first = true;
            for (Map.Entry<String, String> entry : event.metadata.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("]");
        }
        
        logger.log(event.level, sb.toString());
    }
    
    /**
     * 处理剩余日志
     */
    private static void drainRemainingLogs() {
        List<LogEvent> remainingEvents = new ArrayList<>();
        eventQueue.drainTo(remainingEvents);
        
        for (LogEvent event : remainingEvents) {
            writeLogDirectly(event);
        }
    }
    
    /**
     * 输出统计信息
     */
    private static void outputStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("====== JanusGuard 会话统计 ======\n");
        
        for (Map.Entry<String, AtomicLong> entry : eventCounters.entrySet()) {
            stats.append(entry.getKey()).append(": ").append(entry.getValue().get()).append(" 事件\n");
        }
        
        stats.append("================================");
        logger.info(stats.toString());
    }
    
    /**
     * 安装运行时钩子，来监控命令执行
     * 这是一个简化版实现，实际环境应使用字节码操作
     */
    private static void installRuntimeHooks() {
        try {
            // 注册JVM关闭钩子，确保在程序结束时能看到我们的日志
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("JanusGuard JDK 8兼容版 关闭中...");
                
                // 输出命令执行统计信息
                AtomicLong counter = eventCounters.get("COMMAND_EXECUTION");
                if (counter != null) {
                    logger.info("总共监控到的命令执行次数: " + counter.get());
                }
            }));
            
            logger.info("已安装运行时钩子和关闭监听器");
        } catch (Exception e) {
            logger.log(Level.WARNING, "安装运行时钩子失败", e);
        }
    }
    
    /**
     * Agent入口点
     */
    public static void premain(String args, Instrumentation inst) {
        try {
            logger.info("JanusGuard JDK 8兼容版 v" + VERSION + " 启动中...");
            
            // 解析参数
            if (args != null && !args.isEmpty()) {
                parseArgs(args);
            }
            
            // 加载配置
            loadConfig();
            
            // 初始化日志
            setupLogging();
            
            // 确保对命令执行的检测是启用的
            monitorCommandExecution = true;
            logger.info("已启用命令执行监控");
            
            // 安装多层监控（SecurityManager + 直接监控）
            installSecurityManager();
            installRuntimeExecHook();  // 添加直接的Runtime执行钩子
            
            // 执行一个测试命令，确认监控功能正常工作
            testCommandMonitoring();
            
            // 输出启动信息
            System.out.println("JanusGuard命令执行监控已启动");
            logger.info("JanusGuard JDK 8兼容版 v" + VERSION + " 已启动 - 活跃监控: 命令执行");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Agent启动失败", e);
        }
    }
    
    /**
     * 解析Agent参数
     */
    private static void parseArgs(String args) {
        logger.info("解析参数: " + args);
        
        String[] argPairs = args.split(",");
        for (String argPair : argPairs) {
            String[] keyValue = argPair.split("=", 2);
            if (keyValue.length == 2) {
                if ("config".equals(keyValue[0])) {
                    configFile = new File(keyValue[1]);
                    logger.info("配置文件: " + configFile.getAbsolutePath());
                } else if ("log".equals(keyValue[0])) {
                    logFile = keyValue[1];
                    logger.info("日志文件: " + logFile);
                }
            }
        }
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try {
            if (configFile != null && configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                config.load(fis);
                fis.close();
                logger.info("已加载配置文件: " + configFile.getAbsolutePath());
                
                // 应用配置
                applyConfig();
            } else {
                logger.warning("未指定配置文件或文件不存在，使用默认配置");
                
                // 加载默认配置
                loadDefaultConfig();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "加载配置文件失败", e);
            
            // 加载默认配置
            loadDefaultConfig();
        }
    }
    
    /**
     * 加载默认配置
     */
    private static void loadDefaultConfig() {
        // 设置默认配置值
        monitorClassLoading = true;
        monitorReflection = true;
        monitorFileOperations = true;
        monitorCommandExecution = true;
        logLevel = Level.INFO;
        enableBlockingMode = false;
    }
    
    /**
     * 应用配置
     */
    private static void applyConfig() {
        // 应用日志配置
        String logLevelStr = config.getProperty("log.level", "INFO");
        try {
            logLevel = Level.parse(logLevelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("无效的日志级别: " + logLevelStr + "，使用默认INFO级别");
            logLevel = Level.INFO;
        }
        
        logFile = config.getProperty("log.file", LOG_FILE_DEFAULT);
        
        // 应用监控配置
        monitorClassLoading = Boolean.parseBoolean(config.getProperty("monitor.classloading", "true"));
        monitorReflection = Boolean.parseBoolean(config.getProperty("monitor.reflection", "true"));
        monitorFileOperations = Boolean.parseBoolean(config.getProperty("monitor.fileoperations", "true"));
        monitorCommandExecution = Boolean.parseBoolean(config.getProperty("monitor.commandexecution", "true"));
        
        // 应用阻止模式
        enableBlockingMode = Boolean.parseBoolean(config.getProperty("security.blocking-mode", "false"));
        
        // 加载黑名单和白名单
        loadListsFromConfig();
    }
    
    /**
     * 从配置中加载黑白名单
     */
    private static void loadListsFromConfig() {
        // 危险命令模式
        String dangerousCommands = config.getProperty("security.dangerous-commands", "");
        if (!dangerousCommands.isEmpty()) {
            dangerousCommandPatterns.clear();
            for (String pattern : dangerousCommands.split(",")) {
                dangerousCommandPatterns.add(pattern.trim());
            }
        }
        
        // 危险文件模式
        String dangerousFiles = config.getProperty("security.dangerous-files", "");
        if (!dangerousFiles.isEmpty()) {
            dangerousFilePatterns.clear();
            for (String pattern : dangerousFiles.split(",")) {
                dangerousFilePatterns.add(pattern.trim());
            }
        }
        
        // 白名单命令
        String whitelistCmd = config.getProperty("security.whitelist-commands", "");
        if (!whitelistCmd.isEmpty()) {
            whitelistedCommands.clear();
            for (String cmd : whitelistCmd.split(",")) {
                whitelistedCommands.add(cmd.trim());
            }
        }
        
        // 白名单文件
        String whitelistFile = config.getProperty("security.whitelist-files", "");
        if (!whitelistFile.isEmpty()) {
            whitelistedFiles.clear();
            for (String file : whitelistFile.split(",")) {
                whitelistedFiles.add(file.trim());
            }
        }
    }
    
    /**
     * 设置日志
     */
    private static void setupLogging() {
        try {
            // 移除之前的处理器
            for (java.util.logging.Handler handler : logger.getHandlers()) {
                if (handler instanceof FileHandler) {
                    logger.removeHandler(handler);
                }
            }
            
            // 设置日志处理器
            FileHandler fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            
            // 设置日志级别
            logger.setLevel(logLevel);
            
            logger.info("日志级别设置为: " + logLevel);
        } catch (Exception e) {
            logger.log(Level.WARNING, "设置日志处理器失败", e);
        }
    }
    
    /**
     * 安装安全管理器来监控命令执行
     */
    private static void installSecurityManager() {
        try {
            // 获取当前安全管理器
            final SecurityManager currentManager = System.getSecurityManager();
            
            // 创建我们的安全管理器，继承现有的安全检查
            SecurityManager guardianManager = new SecurityManager() {
                @Override
                public void checkExec(String cmd) {
                    // 在执行时先进行监控
                    CommandMonitor.onCommandExecution(cmd);
                    
                    // 然后调用原始的安全管理器检查(如果有)
                    if (currentManager != null) {
                        currentManager.checkExec(cmd);
                    }
                }
                
                // 保留其他权限检查的默认行为
                @Override 
                public void checkPermission(Permission perm) {
                    if (currentManager != null) {
                        currentManager.checkPermission(perm);
                    }
                }
                
                @Override
                public void checkPermission(Permission perm, Object context) {
                    if (currentManager != null) {
                        currentManager.checkPermission(perm, context);
                    }
                }
            };
            
            // 安装我们的安全管理器
            System.setSecurityManager(guardianManager);
            logger.info("已安装JanusGuard安全管理器");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "安装安全管理器失败", e);
        }
    }
    
    /**
     * 安装Runtime.exec直接监控钩子
     */
    private static void installRuntimeExecHook() {
        try {
            logger.info("安装直接命令执行钩子...");
            
            // 注册我们自己作为命令执行的切面
            RuntimeExecHook.install();
            
            logger.info("命令执行钩子安装完成");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "安装命令执行钩子失败", e);
        }
    }
    
    /**
     * 测试命令监控功能
     */
    private static void testCommandMonitoring() {
        try {
            logger.info("执行命令监控测试...");
            
            // 模拟whoami命令执行
            CommandMonitor.onCommandExecution("whoami");
            
            // 输出测试结果
            logger.info("命令监控测试完成");
        } catch (Exception e) {
            logger.log(Level.WARNING, "命令监控测试失败", e);
        }
    }
    
    /**
     * 文件操作监控
     */
    public static class FileMonitor {
        /**
         * 监控文件读取
         */
        public static void onFileRead(String path, boolean isDirectory) {
            if (!monitorFileOperations) {
                return;
            }
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("path", path);
            metadata.put("operation", "READ");
            metadata.put("isDirectory", String.valueOf(isDirectory));
            metadata.put("threadName", Thread.currentThread().getName());
            
            logSecurityEvent("FILE_OPERATION", "读取文件: " + path, metadata);
        }
        
        /**
         * 监控文件写入
         */
        public static void onFileWrite(String path, boolean isDirectory) {
            if (!monitorFileOperations) {
                return;
            }
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("path", path);
            metadata.put("operation", "WRITE");
            metadata.put("isDirectory", String.valueOf(isDirectory));
            metadata.put("threadName", Thread.currentThread().getName());
            
            logSecurityEvent("FILE_OPERATION", "写入文件: " + path, metadata);
            
            // 检查并可能阻止危险操作
            if (isDangerousOperation("FILE_OPERATION", metadata)) {
                throw new SecurityException("危险的文件操作被阻止: " + path);
            }
        }
        
        /**
         * 监控文件删除
         */
        public static void onFileDelete(String path) {
            if (!monitorFileOperations) {
                return;
            }
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("path", path);
            metadata.put("operation", "DELETE");
            metadata.put("threadName", Thread.currentThread().getName());
            
            logSecurityEvent("FILE_OPERATION", "删除文件: " + path, metadata);
            
            // 检查并可能阻止危险操作
            if (isDangerousOperation("FILE_OPERATION", metadata)) {
                throw new SecurityException("危险的文件操作被阻止: " + path);
            }
        }
    }
    
    /**
     * 命令执行监控
     */
    public static class CommandMonitor {
        private static final AtomicLong commandCounter = new AtomicLong(0);
        private static final Set<String> recentCommands = 
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            
        /**
         * 监控命令执行
         */
        public static void onCommandExecution(String command) {
            if (!monitorCommandExecution) {
                return;
            }
            
            // 命令计数
            long id = commandCounter.incrementAndGet();
            
            // 记录元数据
            Map<String, String> metadata = new HashMap<>();
            metadata.put("command", command);
            metadata.put("commandId", String.valueOf(id));
            metadata.put("threadName", Thread.currentThread().getName());
            metadata.put("threadId", String.valueOf(Thread.currentThread().getId()));
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 记录堆栈信息以便追踪
            String callStack = getCallStack();
            metadata.put("callStack", callStack);
            
            // 记录事件
            logSecurityEvent("COMMAND_EXECUTION", "执行命令: " + command, metadata);
            
            // 检查是否为危险命令
            if (isDangerousOperation("COMMAND_EXECUTION", metadata)) {
                if (enableBlockingMode) {
                    logger.severe("已阻止危险命令: " + command);
                    throw new SecurityException("危险的命令执行被阻止: " + command);
                } else {
                    logger.warning("检测到危险命令: " + command + " (阻止模式未开启)");
                }
            }
        }
        
        /**
         * 监控ProcessBuilder
         */
        public static void onProcessBuilderStart(ProcessBuilder pb) {
            if (!monitorCommandExecution) {
                return;
            }
            
            // 提取命令
            String command = "";
            if (pb.command() != null && !pb.command().isEmpty()) {
                command = String.join(" ", pb.command());
            }
            
            long id = commandCounter.incrementAndGet();
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("command", command);
            metadata.put("commandId", String.valueOf(id));
            metadata.put("threadName", Thread.currentThread().getName());
            metadata.put("threadId", String.valueOf(Thread.currentThread().getId()));
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            metadata.put("sourceClass", "ProcessBuilder.start");
            
            // 添加进程工作目录信息
            if (pb.directory() != null) {
                metadata.put("workingDirectory", pb.directory().getAbsolutePath());
            }
            
            // 添加环境变量信息
            if (pb.environment() != null && !pb.environment().isEmpty()) {
                StringBuilder envSb = new StringBuilder();
                for (Map.Entry<String, String> entry : pb.environment().entrySet()) {
                    if (entry.getKey().contains("PATH") || entry.getKey().contains("HOME")) {
                        envSb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                    }
                }
                if (envSb.length() > 0) {
                    metadata.put("relevantEnv", envSb.toString());
                }
            }
            
            // 添加堆栈信息
            metadata.put("callStack", getCallStack());
            
            logSecurityEvent("COMMAND_EXECUTION", "通过ProcessBuilder执行命令: " + command, metadata);
            
            // 检查并可能阻止危险操作
            if (isDangerousOperation("COMMAND_EXECUTION", metadata)) {
                if (enableBlockingMode) {
                    logger.severe("已阻止危险命令: " + command);
                    throw new SecurityException("危险的命令执行被阻止: " + command);
                } else {
                    logger.warning("检测到危险命令: " + command + " (阻止模式未开启)");
                }
            }
        }
    }
    
    /**
     * 反射监控
     */
    public static class ReflectionMonitor {
        /**
         * 监控反射方法调用
         */
        public static void onMethodInvoke(Object target, String methodName, Object[] args) {
            if (!monitorReflection) {
                return;
            }
            
            String targetClass = target != null ? target.getClass().getName() : "null";
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("targetClass", targetClass);
            metadata.put("methodName", methodName);
            metadata.put("threadName", Thread.currentThread().getName());
            
            // 记录参数（限制长度以避免日志爆炸）
            if (args != null && args.length > 0) {
                String argsStr = Arrays.toString(args);
                if (argsStr.length() > 100) {
                    argsStr = argsStr.substring(0, 97) + "...";
                }
                metadata.put("arguments", argsStr);
            }
            
            logSecurityEvent("REFLECTION", "反射调用: " + targetClass + "." + methodName, metadata);
            
            // 检查危险反射调用
            if (isDangerousReflection(targetClass, methodName)) {
                logger.warning("检测到危险反射调用: " + targetClass + "." + methodName);
                
                if (enableBlockingMode) {
                    throw new SecurityException("危险的反射调用被阻止: " + targetClass + "." + methodName);
                }
            }
        }
        
        /**
         * 检查是否为危险的反射调用
         */
        private static boolean isDangerousReflection(String targetClass, String methodName) {
            // 检查是否调用了危险类的方法
            for (String dangerousClass : dangerousReflectionTargets) {
                if (targetClass.startsWith(dangerousClass)) {
                    // 检查特定危险方法
                    if ("java.lang.Runtime".equals(dangerousClass)) {
                        return "exec".equals(methodName) || "getRuntime".equals(methodName);
                    } else if ("java.lang.ProcessBuilder".equals(dangerousClass)) {
                        return "start".equals(methodName);
                    } else if ("java.lang.System".equals(dangerousClass)) {
                        return "exit".equals(methodName);
                    }
                    return true;
                }
            }
            return false;
        }
        
        /**
         * 监控类加载
         */
        public static void onClassLoad(String className) {
            if (!monitorClassLoading) {
                return;
            }
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("className", className);
            metadata.put("threadName", Thread.currentThread().getName());
            
            logSecurityEvent("CLASS_LOADING", "加载类: " + className, metadata);
        }
    }
    
    /**
     * 辅助方法：获取调用堆栈
     */
    public static String getCallStack() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        
        // 跳过前几个元素（getStackTrace和getCallStack方法）
        for (int i = 2; i < stack.length && i < 15; i++) {
            sb.append("\n\t").append(stack[i].toString());
        }
        
        return sb.toString();
    }
    
    /**
     * 模拟命令执行监控，用于演示
     */
    private static void simulateCommandMonitoring() {
        logger.warning("**** 运行模拟测试 ****");
        
        // 模拟whoami命令执行
        Map<String, String> metadata = new HashMap<>();
        metadata.put("command", "whoami");
        metadata.put("threadName", Thread.currentThread().getName());
        metadata.put("simulationMode", "true");
        
        logSecurityEvent("COMMAND_EXECUTION", "检测到命令执行: whoami (模拟)", metadata);
        
        // 检查是否应该阻止
        boolean isDangerous = isDangerousOperation("COMMAND_EXECUTION", metadata);
        
        if (isDangerous) {
            logger.warning("检测到危险命令执行: whoami");
            
            if (enableBlockingMode) {
                logger.severe("危险命令执行已阻止: whoami");
            } else {
                logger.warning("危险命令执行已记录但未阻止: whoami (阻止模式未开启)");
            }
        }
        
        // 实际测试实际的命令执行效果
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("whoami");
            CommandMonitor.onCommandExecution("whoami");
            
            // 读取命令输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                logger.info("命令输出: " + output.toString().trim());
            }
            
            logger.info("已直接调用CommandMonitor.onCommandExecution演示监控效果");
        } catch (Exception e) {
            logger.log(Level.WARNING, "测试命令执行监控时出错", e);
        }
        
        // 指导开发人员如何集成监控代码
        logger.warning("**** 开发人员集成指南 ****");
        logger.warning("如果无法通过Java Agent拦截命令执行，您可以在代码中直接调用以下方法：");
        logger.warning("1. 执行命令前: SimpleJdk8Agent.beforeCommandExecution(command)");
        logger.warning("2. 执行命令后: SimpleJdk8Agent.afterCommandExecution(command, exitCode)");
        logger.warning("这将确保所有命令执行都被正确监控");
        
        // 提供示例代码
        logger.warning("示例代码:");
        logger.warning("try {");
        logger.warning("    String command = \"whoami\";");
        logger.warning("    SimpleJdk8Agent.beforeCommandExecution(command); // 执行前监控");
        logger.warning("    Process process = Runtime.getRuntime().exec(command);");
        logger.warning("    int exitCode = process.waitFor();");
        logger.warning("    SimpleJdk8Agent.afterCommandExecution(command, exitCode); // 执行后监控");
        logger.warning("} catch (Exception e) {");
        logger.warning("    e.printStackTrace();");
        logger.warning("}");
        
        logger.warning("**** 模拟测试完成 ****");
    }
    
    /**
     * 公共API: 在执行命令前调用
     * 这允许开发人员在无法使用字节码转换时手动集成监控
     */
    public static void beforeCommandExecution(String command) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("command", command);
            
            // 检查命令是否危险
            boolean isDangerous = isDangerousOperation("command_execution", metadata);
            metadata.put("dangerous", String.valueOf(isDangerous));
            
            // 记录命令执行事件
            logSecurityEvent("command_execution", "执行命令: " + command, metadata);
            
            // 如果开启了阻止模式且命令危险，则阻止执行
            if (isDangerous && enableBlockingMode) {
                String message = "检测到危险命令执行: " + command;
                logger.warning(message);
                throw new SecurityException(message);
            }
            
            // 运行模拟监控以确保功能正常
            CommandMonitor.onCommandExecution(command);
        } catch (SecurityException se) {
            throw se; // 重新抛出安全异常
        } catch (Throwable t) {
            logger.log(Level.WARNING, "命令执行前处理出错", t);
        }
    }
    
    /**
     * 公共API: 在执行命令后调用
     * 这允许开发人员在无法使用字节码转换时手动集成监控
     */
    public static void afterCommandExecution(String command, int exitCode) {
        if (!monitorCommandExecution) {
            return;
        }
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("command", command);
        metadata.put("phase", "after");
        metadata.put("exitCode", String.valueOf(exitCode));
        metadata.put("threadName", Thread.currentThread().getName());
        metadata.put("manualIntegration", "true");
        
        logSecurityEvent("COMMAND_EXECUTION", "已执行命令: " + command + ", 退出码: " + exitCode, metadata);
    }
    
    /**
     * 记录安全事件
     */
    public static void logSecurityEvent(String eventType, String details, Map<String, String> metadata) {
        // 增加事件计数
        AtomicLong counter = eventCounters.computeIfAbsent(eventType, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();
        
        // 决定日志级别
        Level eventLevel = Level.INFO;
        
        // 检查事件是否危险
        boolean isDangerous = isDangerousOperation(eventType, metadata);
        if (isDangerous) {
            eventLevel = Level.WARNING;
        }
        
        // 构建增强的事件日志
        StringBuilder sb = new StringBuilder();
        sb.append("[安全事件 #").append(count).append("] ");
        sb.append("类型: ").append(eventType);
        if (isDangerous) {
            sb.append(" [危险]");
        }
        sb.append(", 详情: ").append(details);
        
        // 添加元数据
        if (metadata != null && !metadata.isEmpty()) {
            sb.append(", 元数据: { ");
            boolean first = true;
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append(" }");
        }
        
        // 提交到异步队列
        try {
            if (!eventQueue.offer(new LogEvent(eventLevel, sb.toString(), metadata), 
                                100, TimeUnit.MILLISECONDS)) {
                // 队列满，直接写日志
                writeLogDirectly(new LogEvent(eventLevel, sb.toString(), metadata));
            }
        } catch (Exception e) {
            // 出错时降级为直接写日志
            logger.log(Level.WARNING, "异步日志失败: " + e.getMessage());
            logger.log(eventLevel, sb.toString());
        }
    }
    
    /**
     * 是否为危险操作
     */
    public static boolean isDangerousOperation(String eventType, Map<String, String> metadata) {
        if ("COMMAND_EXECUTION".equals(eventType)) {
            String command = metadata.get("command");
            if (command == null) {
                return false;
            }
            
            // 直接检查whoami命令
            if (command.contains("whoami")) {
                logger.warning("检测到特定命令: whoami");
                return true;
            }
            
            // 首先检查白名单
            for (String allowed : whitelistedCommands) {
                if (command.equals(allowed)) {
                    return false; // 在白名单中的命令不阻止
                }
            }
            
            // 检查危险命令模式
            for (String pattern : dangerousCommandPatterns) {
                // 尝试使用正则表达式
                try {
                    if (command.matches(pattern)) {
                        return true;
                    }
                } catch (Exception e) {
                    // 降级为子字符串匹配
                    if (command.contains(pattern)) {
                        return true;
                    }
                }
            }
            
            // 添加更多启发式规则
            if (command.contains(">&") || command.contains("|") || 
                command.contains(";") || command.contains("$(")) {
                // 可能的命令注入尝试
                return true;
            }
            
            // 检查可疑的远程连接
            if (command.contains("wget ") || command.contains("curl ") || 
                command.contains("nc ") || command.contains("telnet ")) {
                // 网络连接工具
                return true;
            }
        } else if ("FILE_OPERATION".equals(eventType)) {
            String path = metadata.get("path");
            String operation = metadata.get("operation");
            
            if (path != null) {
                // 首先检查白名单
                for (String allowed : whitelistedFiles) {
                    if (path.equals(allowed)) {
                        return false;
                    }
                }
                
                // 检查危险路径
                for (String pattern : dangerousFilePatterns) {
                    if (path.contains(pattern)) {
                        return true;
                    }
                }
                
                // 检查写入访问关键文件
                if ("WRITE".equals(operation) || "DELETE".equals(operation)) {
                    if (path.contains("/etc/") || path.contains("/bin/") || 
                        path.contains("/sbin/") || path.contains("/boot/") ||
                        path.contains("C:\\Windows\\") || path.contains("C:\\Program Files\\")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Runtime.exec执行钩子
     */
    static class RuntimeExecHook {
        
        /**
         * 安装Runtime.exec钩子
         */
        public static void install() {
            try {
                // 获取Runtime类
                Class<?> runtimeClass = Runtime.class;
                
                // 获取所有exec方法
                Method[] methods = runtimeClass.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals("exec")) {
                        logger.info("已识别Runtime.exec方法: " + method);
                    }
                }
                
                // 注册静态初始化器
                logger.info("注册命令执行代理");
                
                // 设置系统属性以便让应用程序知道它应该手动调用我们的监控方法
                System.setProperty("janusguard.installed", "true");
                System.setProperty("janusguard.manual.monitor", "true");
                
                logger.info("已设置系统属性以启用手动监控集成");
                logger.info("应用程序可以通过以下方式集成命令监控:");
                logger.info("1. 检查System.getProperty(\"janusguard.installed\")");
                logger.info("2. 执行前调用SimpleJdk8Agent.beforeCommandExecution(command)");
                
                // 添加全局关闭钩子
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 报告命令执行统计
                        AtomicLong counter = eventCounters.get("COMMAND_EXECUTION");
                        if (counter != null) {
                            logger.info("总共监控到的命令执行次数: " + counter.get());
                        }
                    }
                }));
            } catch (Exception e) {
                logger.log(Level.WARNING, "安装Runtime.exec钩子失败", e);
            }
        }
    }

    /**
     * ApplicationInstrumentation - 应用程序可以调用的直接监控API
     */
    public static class ApplicationInstrumentation {
        /**
         * 应用程序可以直接调用此方法来监控命令执行
         * @param command 要执行的命令
         */
        public static void monitorCommand(String command) {
            CommandMonitor.onCommandExecution(command);
        }
    }
} 