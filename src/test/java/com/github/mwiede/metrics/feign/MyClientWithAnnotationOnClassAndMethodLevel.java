package com.github.mwiede.metrics.feign;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

/**
 * Created by mwiedemann on 24.10.2017.
 */
@Timed
@ExceptionMetered
@Metered
interface MyClientWithAnnotationOnClassAndMethodLevel {
    @Timed
    @ExceptionMetered
    @Metered
    void myMethod();
}
