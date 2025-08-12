package com.charserzh.lmt.core.annotation;


import com.charserzh.lmt.core.config.LTBeanImporter;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import({LTBeanImporter.class})
public @interface EnableLMT {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

}
