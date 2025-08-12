package com.charserzh.lmt.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LMT {

    /**
     * 场景码
     * @return 场景码
     */
    String bizSceneCode();
}
