package com.github.mwiede.metrics.feign;

import feign.RequestLine;

/**
 * Created by mwiedemann on 24.10.2017.
 */
interface MyClientWithoutAnnotation {

    @RequestLine("POST /")
    void myMethod();
}
