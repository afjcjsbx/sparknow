package com.afjcjsbx.sparknow

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import org.apache.spark.SparkConf
import org.apache.spark.scheduler.TaskLocality
import org.slf4j.{Logger, LoggerFactory}

object Utils {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  private val objectMapper = new ObjectMapper() with ClassTagExtensions
  objectMapper.registerModule(DefaultScalaModule)
  private val objectWriter = objectMapper.writer(new DefaultPrettyPrinter())

  def writeToStringSerializedJSON(metricsData: AnyRef): String = {
    objectWriter.writeValueAsString(metricsData)
  }

  def encodeTaskLocality(taskLocality: TaskLocality.TaskLocality): Int = {
    taskLocality match {
      case TaskLocality.PROCESS_LOCAL => 0
      case TaskLocality.NODE_LOCAL => 1
      case TaskLocality.RACK_LOCAL => 2
      case TaskLocality.NO_PREF => 3
      case TaskLocality.ANY => 4
      case _ => -1
    }
  }

  def parseExtendedModeConfig(conf: SparkConf): Boolean = {
    val extendedMode = conf
      .getOption("spark.sparknow.extendedmode").exists(_.toBoolean)
    logger.info(s"Extended mode: $extendedMode")
    extendedMode
  }

  def parsePushGatewayConfig(conf: SparkConf): String = {
    val url = conf
      .getOption("spark.sparknow.pushgateway")
      .getOrElse(throw new IllegalArgumentException(
        "SERVER:PORT configuration for the Prometheus Push Gateway is required, use --conf spark.sparknow.pushgateway=SERVER:PORT"))
    logger.info(s"Prometheus Push Gateway server and port: $url")
    url
  }

  def parseJobName(conf: SparkConf): String = {
    val jobName = conf.get("spark.sparknow.jobname", "no-job-name")
    logger.info(s"Job Name: $jobName")
    jobName
  }

  def parseKafkaBroker(conf: SparkConf): String = {
    val broker = conf
      .getOption("spark.sparknow.kafkaBroker")
      .getOrElse(
        throw new IllegalArgumentException("Kafka broker is required for the Kafka connection"))
    logger.info(s"Kafka broker: $broker")
    broker
  }

  def parseKafkaTopic(conf: SparkConf): String = {
    val topic = conf
      .getOption("spark.sparknow.kafkaTopic")
      .getOrElse(
        throw new IllegalArgumentException("Kafka topic is required for the Kafka connection"))
    logger.info(s"Kafka topic: $topic")
    topic
  }
}
