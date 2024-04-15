package com.afjcjsbx.sparknow

import org.apache.spark.SparkConf
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/** A Spark listener that sends metrics data to a Prometheus Push Gateway.
  *
  * This listener captures various Spark events, such as stage completion, job start/end, and SQL
  * execution, and reports corresponding metrics to a Prometheus Push Gateway.
  *
  * @param conf
  *   The Spark configuration containing settings for connecting to the Push Gateway.
  */
final class PushGatewaySink(conf: SparkConf) extends AbstractSink(conf) {
  override val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  private val gateway = initGateway()

  logger.warn(
    "Custom monitoring listener with Prometheus Push Gateway sink initializing. Now attempting to connect to the Push Gateway")

  private def initGateway(): PushGateway = {
    val url: String = Utils.parsePushGatewayConfig(conf)
    val jobName: String = Utils.parseJobName(conf)
    PushGateway(url, jobName)
  }

  override def report(metricsContainer: MetricsContainer): Unit = {
    val strMetrics = metricsContainer.getMetrics
      .collect { case (metric: String, value: Long) =>
        gateway.validateMetric(metric.toLowerCase()) + " " + value.toString
      }
      .mkString("\n")
      .concat("\n")

    Try {
      gateway
        .post(strMetrics, metricsContainer.metricType, "appid", appId)
    }.recover { case ex: Throwable =>
      logger.error(s"Error on reporting metrics to Push Gateway: ${ex.getMessage}", ex)
    }
  }
}
