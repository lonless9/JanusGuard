import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * JanusGuard集成助手类
 * 应用程序可以使用此类来轻松集成JanusGuard的安全监控功能
 */
public class JanusGuardHelper {
    private static final boolean isJanusGuardInstalled;
    
    static {
        // 检查JanusGuard是否已安装
        isJanusGuardInstalled = Boolean.parseBoolean(
            System.getProperty("janusguard.installed", "false"));
        
        if (isJanusGuardInstalled) {
            System.out.println("JanusGuard监控已加载，安全增强功能可用");
        }
    }
    
    /**
     * 执行命令并确保被JanusGuard监控
     * @param command 要执行的命令
     * @return 命令执行的进程
     * @throws IOException 如果命令执行失败
     */
    public static Process execCommand(String command) throws IOException {
        // 如果JanusGuard已安装，通知它我们要执行命令
        if (isJanusGuardInstalled) {
            try {
                // 尝试调用SimpleJdk8Agent的命令监控方法
                Class<?> agentClass = Class.forName("SimpleJdk8Agent");
                java.lang.reflect.Method monitorMethod = 
                    agentClass.getMethod("beforeCommandExecution", String.class);
                monitorMethod.invoke(null, command);
            } catch (Exception e) {
                System.err.println("JanusGuard监控API调用失败: " + e.getMessage());
            }
        }
        
        // 执行实际命令
        return Runtime.getRuntime().exec(command);
    }
    
    /**
     * 执行命令数组并确保被JanusGuard监控
     * @param cmdarray 命令数组
     * @return 命令执行的进程
     * @throws IOException 如果命令执行失败
     */
    public static Process execCommand(String[] cmdarray) throws IOException {
        // 如果JanusGuard已安装，通知它我们要执行命令
        if (isJanusGuardInstalled) {
            try {
                // 将命令数组转换为单个字符串进行监控
                String command = String.join(" ", cmdarray);
                
                // 尝试调用SimpleJdk8Agent的命令监控方法
                Class<?> agentClass = Class.forName("SimpleJdk8Agent");
                java.lang.reflect.Method monitorMethod = 
                    agentClass.getMethod("beforeCommandExecution", String.class);
                monitorMethod.invoke(null, command);
            } catch (Exception e) {
                System.err.println("JanusGuard监控API调用失败: " + e.getMessage());
            }
        }
        
        // 执行实际命令
        return Runtime.getRuntime().exec(cmdarray);
    }
    
    /**
     * 检查JanusGuard是否已安装
     * @return 如果JanusGuard已安装则返回true
     */
    public static boolean isJanusGuardInstalled() {
        return isJanusGuardInstalled;
    }
} 