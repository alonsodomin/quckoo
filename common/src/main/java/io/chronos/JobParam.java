package io.chronos;

import java.lang.annotation.*;

/**
 * Created by aalonsodominguez on 12/07/15.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface JobParam {
}
