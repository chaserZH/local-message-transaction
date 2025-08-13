package com.charserzh.lmt.core.config;

import com.charserzh.lmt.core.annotation.LMT;
import com.charserzh.lmt.core.callback.LTCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 核心处理器 - LTCallbackBeanPostProcessor
 * 三重身份
 * 1. AOP代理创建器（继承AbstractAutoProxyCreator）
 * 2. Bean定义后处理器（实现BeanDefinitionRegistryPostProcessor）
 * 3. 环境感知组件（实现EnvironmentAware/ApplicationContextAware）
 */
public class LTCallbackBeanPostProcessor extends AbstractAutoProxyCreator implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(LTCallbackBeanPostProcessor.class);

    private final Set<String> packagesToScan;

    private Environment environment;

    private ApplicationContext applicationContext;


    public LTCallbackBeanPostProcessor(String... packagesToScan) {
        this.packagesToScan = new LinkedHashSet<>(Arrays.asList(packagesToScan));
    }

    /**
     * 阶段4. 代理创建
     *  getAdvicesAndAdvisorsForBean,对实现了LTCallback接口的Bean，添加LTCallbackMethodInterceptor拦截器
     *
     * @param beanClass 目标Bean的类
     * @param beanName 目标Bean的名称
     * @param customTargetSource 自定义目标源
     * @return 拦截器数组
     * @throws BeansException 异常
     */
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        if (com.charserzh.lmt.core.callback.LTCallback.class.isAssignableFrom(beanClass)){
            return new Object[] { this.applicationContext.getBean(LTCallbackMethodInterceptor.class) };
        }
        return DO_NOT_PROXY;
    }

    /**
     * 阶段3. Bean处理（LTCallbackBeanPostProcessor）
     * 解析环境变量占位符（如${some.path}）
     * 使用ClassPathBeanDefinitionScanner扫描指定包下带有@LMT注解的类
     * @param registry Bean定义注册器
     * @throws BeansException 异常
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> resolvePackageToScan = resolvePackageToScan(this.packagesToScan);
        if (!CollectionUtils.isEmpty(resolvePackageToScan)) {
            registerLTBean(resolvePackageToScan, registry);
        } else {
            log.warn("package scan is empty,can not find any packages to register LT Bean,ignore");
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("LTCallbackBeanPostProcessor: postProcessBeanFactory executed");
    }

    /**
     * 注册LT Bean
     * @param resolvedPackagesToScan 解析后的包
     * @param registry Bean定义注册器
     */
    private void registerLTBean(Set<String> resolvedPackagesToScan, BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.addIncludeFilter(new AnnotationTypeFilter(LMT.class));
        for (String packageToScan : resolvedPackagesToScan) {
            scanner.scan(packageToScan);
        }
    }

    /**
     * 解析需要扫描的包
     * @param packagesToScan 注解扫描的包
     * @return 解析后的包
     */
    private Set<String> resolvePackageToScan(Set<String> packagesToScan) {
        LinkedHashSet<String> resolvedPackagesToScan = new LinkedHashSet<>(packagesToScan.size());
        if (environment == null) {
            return resolvedPackagesToScan;
        }

        for (String packageToScan : packagesToScan) {
            if (StringUtils.hasText(packageToScan)) {
                resolvedPackagesToScan.add(this.environment.resolvePlaceholders(packageToScan));
            }
        }
        return resolvedPackagesToScan;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
