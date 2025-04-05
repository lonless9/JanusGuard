# Java内存木马检测示例

本文档提供了几种常见Java内存木马注入方式的检测示例，展示JanusGuard的监控能力。

## 1. ClassLoader注入检测

### 恶意类动态加载示例

```java
public class ClassLoaderTrojanDemo {
    public static void main(String[] args) throws Exception {
        // 准备恶意类字节码
        byte[] evilClassBytes = getEvilClassBytes();
        
        // 使用自定义ClassLoader加载恶意类
        ClassLoader classLoader = new URLClassLoader(new URL[0], 
            ClassLoaderTrojanDemo.class.getClassLoader());
        
        // 使用反射调用defineClass方法
        Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
            "defineClass", String.class, byte[].class, int.class, int.class);
        defineClassMethod.setAccessible(true);
        
        // 定义恶意类 - 此时JanusGuard将检测到可疑行为
        Class<?> evilClass = (Class<?>) defineClassMethod.invoke(classLoader,
            "EvilCommand", evilClassBytes, 0, evilClassBytes.length);
        
        // 实例化并执行恶意类
        Object evilInstance = evilClass.newInstance();
        Method executeMethod = evilClass.getMethod("execute");
        executeMethod.invoke(evilInstance);
    }
    
    // 获取恶意类字节码的方法
    private static byte[] getEvilClassBytes() {
        // 实际中可能从网络下载或其他隐蔽途径获取字节码
        // ...
        return new byte[1024]; // 示例
    }
}
```

### JanusGuard检测结果

当上述代码执行时，JanusGuard将生成类似以下的告警日志：

```
[WARN] [2023-04-04 15:30:12] [JanusGuard] - 内存木马可疑活动: 检测到可疑类加载 
{
  "id": "61f2a3b4-c8e5-4e21-9d26-8c3a8b7f6d45",
  "type": "CLASS_LOADING",
  "severity": "HIGH",
  "className": "java.net.URLClassLoader",
  "methodName": "defineClass",
  "threadName": "main",
  "timestamp": 1680609012345,
  "data": {
    "targetClass": "EvilCommand",
    "classLoader": "java.net.URLClassLoader",
    "memoryTrojanSuspicious": true,
    "reason": "可疑类动态加载检测",
    "callStack": "java.lang.reflect.Method.invoke(...)\nClassLoaderTrojanDemo.main(...)"
  }
}
```

## 2. Unsafe内存操作检测

### Unsafe内存注入示例

```java
public class UnsafeTrojanDemo {
    public static void main(String[] args) throws Exception {
        // 获取Unsafe实例
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        
        // 创建恶意类实例，绕过构造函数检查
        Object evilObject = unsafe.allocateInstance(SecuredClass.class);
        
        // 修改私有字段，绕过安全检查
        Field securityField = SecuredClass.class.getDeclaredField("securityCheckPassed");
        long offset = unsafe.objectFieldOffset(securityField);
        
        // 直接修改内存 - 此时JanusGuard将检测到可疑行为
        unsafe.putBoolean(evilObject, offset, true);
        
        // 调用不应被调用的方法
        Method dangerousMethod = SecuredClass.class.getDeclaredMethod("dangerousOperation");
        dangerousMethod.setAccessible(true);
        dangerousMethod.invoke(evilObject);
    }
    
    static class SecuredClass {
        private boolean securityCheckPassed = false;
        
        public SecuredClass() {
            // 正常构造过程会进行安全验证
            // securityCheckPassed 只有通过验证才会为true
        }
        
        public void dangerousOperation() {
            if (securityCheckPassed) {
                System.out.println("执行危险操作");
                // 执行特权操作...
            } else {
                throw new SecurityException("安全检查失败");
            }
        }
    }
}
```

### JanusGuard检测结果

```
[WARN] [2023-04-04 15:35:28] [JanusGuard] - 检测到高危Unsafe操作: putBoolean
{
  "id": "72f3b4c5-d9e6-5f32-0e37-9d4a9c8g7e56",
  "type": "JVM_MEMORY_OPERATION",
  "severity": "HIGH",
  "className": "sun.misc.Unsafe",
  "methodName": "putBoolean",
  "threadName": "main",
  "timestamp": 1680609328765,
  "data": {
    "unsafeMethod": "putBoolean",
    "dangerousOperation": true,
    "reason": "高危Unsafe内存操作",
    "callStack": "UnsafeTrojanDemo.main(...)"
  }
}
```

## 3. 动态代理木马检测

### 恶意代理注入示例

```java
public class ProxyTrojanDemo {
    public static void main(String[] args) {
        // 创建恶意的InvocationHandler
        InvocationHandler evilHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 恶意代码，如果是特定方法则执行木马行为
                if (method.getName().equals("doFilter")) {
                    // 获取请求和响应对象
                    Object request = args[0];
                    Object response = args[1];
                    
                    // 执行木马功能，如后门命令执行
                    String cmd = getParameter(request, "cmd");
                    if (cmd != null && !cmd.isEmpty()) {
                        executeCommand(cmd, response);
                        return null; // 短路，不调用真实方法
                    }
                }
                // 否则调用原始方法
                return method.invoke(args[2], args);
            }
            
            private String getParameter(Object request, String name) {
                // 从请求中获取参数
                // ...
                return null; // 示例
            }
            
            private void executeCommand(String cmd, Object response) {
                // 执行命令并将结果写回响应
                // ...
            }
        };
        
        // 创建动态代理 - 此时JanusGuard将检测到可疑行为
        Object evilProxy = Proxy.newProxyInstance(
            ProxyTrojanDemo.class.getClassLoader(),
            new Class[] { Filter.class },
            evilHandler
        );
        
        // 注册到过滤器链中
        // ...
    }
    
    // 模拟Filter接口
    interface Filter {
        void doFilter(Object request, Object response, Object chain);
    }
}
```

### JanusGuard检测结果

```
[WARN] [2023-04-04 15:40:45] [JanusGuard] - 检测到可疑动态代理创建, 接口: javax.servlet.Filter
{
  "id": "83g4c5d6-e0f7-6g43-1f48-0e5b0d9h8f67",
  "type": "DYNAMIC_PROXY",
  "severity": "HIGH",
  "className": "java.lang.reflect.Proxy",
  "methodName": "newProxyInstance",
  "threadName": "main",
  "timestamp": 1680609645123,
  "data": {
    "classLoader": "app.ProxyTrojanDemo$ClassLoader",
    "interfaces": "javax.servlet.Filter",
    "suspiciousProxy": true,
    "reason": "可疑动态代理创建",
    "callStack": "ProxyTrojanDemo.main(...)"
  }
}
```

## 4. JNI木马检测

### 恶意JNI库加载示例

```java
public class JNITrojanDemo {
    public static void main(String[] args) throws Exception {
        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File evilLib = new File(tempDir, "evil" + System.currentTimeMillis() + ".dll");
        
        // 将恶意库从资源提取到临时目录
        try (InputStream in = JNITrojanDemo.class.getResourceAsStream("/evil.dll");
             FileOutputStream out = new FileOutputStream(evilLib)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        
        // 加载恶意库 - 此时JanusGuard将检测到可疑行为
        System.load(evilLib.getAbsolutePath());
        
        // 调用原生方法
        evilNativeMethod();
        
        // 清理
        evilLib.delete();
    }
    
    // 声明原生方法
    private static native void evilNativeMethod();
}
```

### JanusGuard检测结果

```
[WARN] [2023-04-04 15:45:32] [JanusGuard] - 检测到可疑JNI库加载: C:\Users\user\AppData\Local\Temp\evil1680609932123.dll
{
  "id": "94h5d6e7-f1g8-7h54-2g59-1f6c1e0i9g78",
  "type": "JNI_OPERATION",
  "severity": "HIGH",
  "className": "java.lang.System",
  "methodName": "load",
  "threadName": "main",
  "timestamp": 1680609932123,
  "data": {
    "libraryPath": "C:\\Users\\user\\AppData\\Local\\Temp\\evil1680609932123.dll",
    "suspiciousJNI": true,
    "reason": "可疑JNI库加载",
    "callStack": "JNITrojanDemo.main(...)"
  }
}
```

## 5. 综合检测与防御建议

除上述单独检测外，JanusGuard还具备以下综合检测能力：

1. **关联分析**：关联多个不同类型的可疑事件，识别复杂攻击链
2. **行为基线**：建立应用正常行为基线，检测异常偏差
3. **上下文感知**：结合应用上下文判断操作合法性

针对内存木马防御的最佳实践：

1. 严格控制反射和类加载权限
2. 禁用非必要的JNI调用
3. 采用白名单策略，限制动态加载行为
4. 启用安全管理器，限制敏感操作
5. 定期检查应用依赖的安全性
6. 利用JanusGuard进行实时监控和告警 