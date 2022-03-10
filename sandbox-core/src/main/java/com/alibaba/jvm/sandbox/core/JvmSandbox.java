package com.alibaba.jvm.sandbox.core;

import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandler;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultProviderManager;
import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * 沙箱
 */
public class JvmSandbox {

    /**
     * 需要提前加载的sandbox工具类
     */
    private final static List<String> earlyLoadSandboxClassNameList = new ArrayList<String>();

    static {
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.SandboxClassUtils");
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByAsm");
    }

    private final CoreConfigure cfg;
    private final CoreModuleManager coreModuleManager;

    public JvmSandbox(final CoreConfigure cfg,
                      final Instrumentation inst) {
        //获取事件处理类实例
        EventListenerHandler.getSingleton();
        this.cfg = cfg;
        //初始化模块管理实例
        //1.通过new DefaultProviderManager(cfg)对默认服务提供管理器实现进行实例化。主要是创建了一个针对服务提供库sandbox-mgr-provider.jar的ClassLoader，sandbox-mgr-provider中的类通过JAVA SPI的方式实现可扩展性(w)
        //2.初始化模块目录，包括/Users/zhengmaoshao/sandbox/bin/../module文件夹中系统模块和/Users/zhengmaoshao/.sandbox-module文件夹中的用户自定义模块（w）
        this.coreModuleManager = SandboxProtector.instance.protectProxy(CoreModuleManager.class, new DefaultCoreModuleManager(
                cfg,
                inst,
                new DefaultCoreLoadedClassDataSource(inst, cfg.isEnableUnsafe()),
                new DefaultProviderManager(cfg)
        ));
        //初始化spy类
        init();
    }

    private void init() {
        doEarlyLoadSandboxClass();
        SpyUtils.init(cfg.getNamespace());
    }

    /**
     * 提前加载某些必要的类
     */
    private void doEarlyLoadSandboxClass() {
        for(String className : earlyLoadSandboxClassNameList){
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                //加载sandbox内部的类，不可能加载不到
            }
        }
    }

    /**
     * 获取模块管理器
     *
     * @return 模块管理器
     */
    public CoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    /**
     * 销毁沙箱
     */
    public void destroy() {

        // 卸载所有的模块
        coreModuleManager.unloadAll();

        // 清理Spy
        SpyUtils.clean(cfg.getNamespace());

    }

}
