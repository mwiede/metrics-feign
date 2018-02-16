package com.github.mwiede.metrics.feign;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.ResponseMetered;
import com.codahale.metrics.annotation.Timed;

import feign.RequestLine;

/**
 * Created by mwiedemann on 24.10.2017.
 */
@Timed
@ExceptionMetered
@Metered
@ResponseMetered
interface MyClientWithAnnotationOnClassLevel {

  @RequestLine("POST /")
  void myMethod();
}
