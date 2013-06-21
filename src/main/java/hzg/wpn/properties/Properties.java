package hzg.wpn.properties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Properties {
    /**
     * The value will be used to load defined properties file as resource stream
     *
     * @return
     */
    String resource() default "";

    /**
     * Path to the .properties file. This file will be loaded as a stream.
     *
     * @return
     */
    String file() default "";
}
