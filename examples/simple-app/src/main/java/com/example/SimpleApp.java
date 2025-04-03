package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

/**
 * 简单的测试应用，用于测试JanusGuard Agent的监控功能
 */
public class SimpleApp {

    public static void main(String[] args) {
        System.out.println("======= JanusGuard Agent Test Application =======");
        
        try {
            // 测试命令执行监控
            testCommandExecution();
            
            // 测试文件操作监控
            testFileOperations();
            
            // 测试反射调用监控
            testReflection();
            
            System.out.println("所有测试完成!");
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试命令执行监控
     */
    private static void testCommandExecution() throws IOException {
        System.out.println("\n--- 测试命令执行监控 ---");
        
        // 使用Runtime.exec执行命令
        System.out.println("使用 Runtime.exec 执行 'ls -la'...");
        Process process1 = Runtime.getRuntime().exec("ls -la");
        printProcessOutput(process1);
        
        // 使用ProcessBuilder执行命令
        System.out.println("使用 ProcessBuilder 执行 'echo Hello JanusGuard'...");
        ProcessBuilder pb = new ProcessBuilder("echo", "Hello", "JanusGuard");
        Process process2 = pb.start();
        printProcessOutput(process2);
    }
    
    /**
     * 测试文件操作监控
     */
    private static void testFileOperations() throws IOException {
        System.out.println("\n--- 测试文件操作监控 ---");
        
        // 创建测试文件
        File testFile = new File("test-file.txt");
        System.out.println("创建并写入测试文件: " + testFile.getAbsolutePath());
        
        // 使用FileOutputStream写入文件
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            String content = "This is a test file for JanusGuard Agent.";
            fos.write(content.getBytes());
        }
        
        // 使用FileInputStream读取文件
        System.out.println("读取测试文件内容...");
        try (FileInputStream fis = new FileInputStream(testFile)) {
            byte[] buffer = new byte[1024];
            int length = fis.read(buffer);
            System.out.println("文件内容: " + new String(buffer, 0, length));
        }
        
        // 使用RandomAccessFile读写文件
        System.out.println("使用RandomAccessFile追加内容...");
        try (RandomAccessFile raf = new RandomAccessFile(testFile, "rw")) {
            raf.seek(raf.length());
            raf.writeBytes("\nThis line was added using RandomAccessFile.");
        }
        
        // 删除测试文件
        System.out.println("删除测试文件...");
        if (testFile.delete()) {
            System.out.println("测试文件删除成功");
        } else {
            System.out.println("测试文件删除失败");
        }
    }
    
    /**
     * 测试反射调用监控
     */
    private static void testReflection() throws Exception {
        System.out.println("\n--- 测试反射调用监控 ---");
        
        // 获取当前类的方法
        System.out.println("通过反射调用私有方法...");
        Method method = SimpleApp.class.getDeclaredMethod("privateMethod", String.class);
        method.setAccessible(true);
        
        // 调用私有方法
        String result = (String) method.invoke(null, "JanusGuard");
        System.out.println("反射调用结果: " + result);
    }
    
    /**
     * 私有方法，用于反射调用测试
     */
    private static String privateMethod(String name) {
        return "Hello, " + name + "! This message is from a private method called via reflection.";
    }
    
    /**
     * 输出进程的标准输出和错误输出
     */
    private static void printProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  > " + line);
            }
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("  > " + line);
            }
        }
    }
} 