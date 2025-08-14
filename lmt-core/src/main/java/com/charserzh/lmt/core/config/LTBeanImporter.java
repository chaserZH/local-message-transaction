package com.charserzh.lmt.core.config;

import com.charserzh.lmt.core.annotation.EnableLMT;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * LMT的Bean注册 入口类

 * 通过Spring的ImportBeanDefinitionRegistrar接口动态注册Bean定义。
 * @author zhanghao
 * 自动扫描 @LMT 注解并注册 LTCallbackBeanPostProcessor
 */
public class LTBeanImporter implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> packagesToScan = getAnnotationScanPackages(importingClassMetadata);
        if (!packagesToScan.isEmpty()) {
            registerLTCallbackBeanPostProcessor(packagesToScan, registry);
        }
    }

    /**
     * 注册 LTCallbackBeanPostProcessor
     * @param packagesToScan 扫描的包名
     * @param registry BeanDefinitionRegistry
     */
    private void registerLTCallbackBeanPostProcessor(Set<String> packagesToScan, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .rootBeanDefinition(LTCallbackBeanPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        builder.setScope("singleton");
        builder.setRole(2);
        BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
    }

    /**
     * 获取 @EnableLMT 注解的扫描包名
     * @param importingClassMetadata 导入类的元数据
     * @return 扫描的包名
     */
    private Set<String> getAnnotationScanPackages(AnnotationMetadata importingClassMetadata) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableLMT.class.getName())
        );
        if (Objects.nonNull(attrs)) {
            Set<String> packageToScan = new LinkedHashSet<>();

            // value 和 basePackages 永远不为 null
            packageToScan.addAll(Arrays.asList(attrs.getStringArray("value")));
            packageToScan.addAll(Arrays.asList(attrs.getStringArray("basePackages")));

            Arrays.stream(attrs.getClassArray("basePackageClasses"))
                    .map(ClassUtils::getPackageName)
                    .forEach(packageToScan::add);

            // 如果没有配置，默认扫描导入类所在包
            if (packageToScan.isEmpty()) {
                // 默认加上 com.charserzh.lmt
                packageToScan.add("com.charserzh.lmt");
                packageToScan.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
            }
            return packageToScan;
        }
        return Collections.emptySet();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
