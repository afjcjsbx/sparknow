package com.afjcjsbx.sparknow

import scala.collection.mutable

case class MetricsContainer(metricType: String) {
  private val metrics: mutable.Map[String, Any] = mutable.Map[String, Any]()

  def add(key: String, value: Any): MetricsContainer = {
    metrics += (key -> value)
    this
  }

  def add(tuple: (String, Any)): MetricsContainer = {
    metrics += tuple
    this
  }

  def getMetrics: mutable.Map[String, Any] = {
    metrics
  }
}
