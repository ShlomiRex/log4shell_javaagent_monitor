package com.shlomirex;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent {
    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("Java Agent: Premain");
        instrument(agentOps, inst);
    }

    private static void instrument(String agentOps, Instrumentation inst) {
        System.out.println("Parameters: " + agentOps);
        inst.addTransformer(new MyTransformer());
    }

    static class MyTransformer implements ClassFileTransformer {

        // This will log the jndi requests
        static final String TARGET_CLASS_NAME = "org/apache/logging/log4j/core/lookup/AbstractLookup";

        // This will log the IP address of the client / sender
        static final String TARGET_CLASS_NAME2 = "org/springframework/web/method/support/InvocableHandlerMethod";
        static final String TARGET_CLASS_NAME2_METHOD = "invokeForRequest";

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (className == null) {
                return null;
            }

            if (className.equals(TARGET_CLASS_NAME)) {
                System.out.println("Found class: " + className);
                ClassPool classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader));
                classPool.appendSystemPath();
                try {
                    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                    for (CtMethod declaredMethod : declaredMethods) {
                        System.out.println("Method modified: " + declaredMethod.getName());

                        declaredMethod.insertBefore(
                                "java.net.Socket s = new java.net.Socket(\"my_monitor_server\", 5000); " +
                                        "java.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream()); " +
                                        "dos.write(\"Method called: "+declaredMethod.getName()+"\".getBytes());" +
                                        "dos.write(\"\\n\".getBytes());" +
                                        "dos.write((\"Arguments: \" + java.util.Arrays.toString($args)).getBytes());" +
                                        "dos.write(\"\\n\".getBytes());" +
                                        "dos.write((\"Process ID: \"+java.lang.management.ManagementFactory.getRuntimeMXBean().getName()).getBytes());" +
                                        "dos.write(\"\\n\".getBytes());" +
                                        "dos.flush();" +
                                        "s.close();");
                    }
                    return ctClass.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (className.equals(TARGET_CLASS_NAME2)) {
                System.out.println("Found class: " + className);
                ClassPool classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader));
                classPool.appendSystemPath();
                try {
                    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                    for (CtMethod declaredMethod : declaredMethods) {
                        if (declaredMethod.getName().equals(TARGET_CLASS_NAME2_METHOD)) {
                            System.out.println("Method modified: " + declaredMethod.getName());

                            declaredMethod.insertBefore(
                                    "java.net.Socket s = new java.net.Socket(\"my_monitor_server\", 5000); " +
                                            "java.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream()); " +
                                            "dos.write(\"Method called: "+declaredMethod.getName()+"\".getBytes());" +
                                            "dos.write(\"\\n\".getBytes());" +
                                            "dos.write((\"Client address: \" + $1).getBytes());" +
                                            "dos.write(\"\\n\".getBytes());" +
                                            "dos.flush();" +
                                            "s.close();");
                        }
                    }
                    return ctClass.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return classfileBuffer;
        }
    }
}
