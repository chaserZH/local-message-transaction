package com.charserzh.lmt.core.config;

import com.charserzh.lmt.core.annotation.EnableLMT;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * LMT的Bean注册 入口类

 * 通过Spring的ImportBeanDefinitionRegistrar接口动态注册Bean定义。
 * @author zhanghao
 */
public class LTBeanImporter implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 从@EnableLMT注解提取扫描路径，注册后置处理器。
        Set<String> annotationScanPackages = getAnnotationScanPackages(importingClassMetadata);
        // 注册后置处理器
        registerTransactionAnnotationBeanPostProcessor(annotationScanPackages, registry);


    }

    /**
     * 阶段2: 注册后置处理器
     *  1.创建LTCallbackBeanPostProcessor的Bean定义
     *  2.将扫描路径通过构造函数传入
     *
     * 注册事务注解BeanPostProcessor
     *  核心处理器 - LTCallbackBeanPostProcessor
     * @param annotationScanPackages 注解扫描的包
     * @param registry Bean定义注册器
     */
    private void registerTransactionAnnotationBeanPostProcessor(Set<String> annotationScanPackages, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(LTCallbackBeanPostProcessor.class);
        builder.addConstructorArgValue(annotationScanPackages);
        builder.setScope("singleton");
        builder.setRole(2);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }

    /**
     * 阶段1 ： 初始化扫描路径
     *  从@EnableLMT注解解析basePackages、basePackageClasses等属性
     *  默认使用导入类所在包路径
     * @param importingClassMetadata 注解元数据
     * @return 注解扫描的包
     */
    private Set<String> getAnnotationScanPackages(AnnotationMetadata importingClassMetadata) {

        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableLMT.class.getName()));
        if (Objects.nonNull(annotationAttributes)) {
            String[] basePackages = annotationAttributes.getStringArray("basePackages");
            Class<?>[] basePackageClasses = annotationAttributes.getClassArray("basePackageClasses");
            String[] value = annotationAttributes.getStringArray("value");
            Set<String> packageToScan = new LinkedHashSet<>(Arrays.asList(value));
            packageToScan.addAll(Arrays.asList(basePackages));
            Arrays.stream(basePackageClasses).forEach(clazz -> packageToScan.add(ClassUtils.getPackageName(clazz)));
            if (packageToScan.isEmpty()) {
                packageToScan.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
            }
            return packageToScan;
        }
        return Collections.emptySet();

    }
}
