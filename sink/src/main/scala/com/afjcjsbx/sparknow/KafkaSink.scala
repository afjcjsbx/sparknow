package com.afjcjsbx.sparknow

import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.spark.SparkConf
import org.apache.spark.scheduler._
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.util.Try

final class KafkaSink(conf: SparkConf) extends AbstractSink(conf) {
  override val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  logger.warn(
    "Custom monitoring listener with Kafka sink initializing. Now attempting to connect to Kafka topic")

  // Initialize Kafka connection
  private val broker: String = Utils.parseKafkaBroker(conf)
  private val topic: String = Utils.parseKafkaTopic(conf)
  private val producer: Producer[String, Array[Byte]] = initializeProducer()

  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    logger.info(
      s"Spark application ended, timestamp = ${applicationEnd.time}, closing Kafka connection.")
    synchronized {
      producer.flush()
      // producer.close()
    }
  }

  private def initializeProducer(): KafkaProducer[String, Array[Byte]] = {
    val props = new Properties()
    props.put("bootstrap.servers", broker)
    props.put("retries", "10")
    props.put("batch.size", "16384")
    props.put("linger.ms", "0")
    props.put("buffer.memory", "16384000")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", classOf[ByteArraySerializer].getName)
    props.put("client.id", "sparknow")
    new KafkaProducer(props)
  }

  override protected def report(metricsContainer: MetricsContainer): Unit = Try {
    val str = Utils.writeToStringSerializedJSON(metricsContainer.getMetrics)
    val message = str.getBytes(StandardCharsets.UTF_8)
    producer.send(new ProducerRecord[String, Array[Byte]](topic, message))
  }.recover { case ex: Throwable =>
    logger.error(s"error on reporting metrics to kafka stream, details=${ex.getMessage}", ex)
  }
}
