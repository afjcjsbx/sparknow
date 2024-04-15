package com.afjcjsbx.sparknow

import cats.effect.unsafe.{IORuntime, IORuntimeBuilder}
import cats.effect.{ExitCode, IO, Sync}
import org.http4s._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.`Content-Type`
import org.slf4j.{Logger, LoggerFactory}
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** Utility class for managing push to a PushGateway server. This class provides methods for
  * sending metrics to the Prometheus PushGateway using HTTP4s.
  *
  * @param serverIPnPort
  *   IP address and port of the PushGateway server.
  * @param metricsJob
  *   Name of the job for metrics.
  */
case class PushGateway(serverIPnPort: String, metricsJob: String) {
  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

  implicit val ioRuntime: IORuntime =
    IORuntimeBuilder
      .apply()
      .setFailureReporter(_ => ())
      .build()

  implicit def loggerFactory[F[_]: Sync]: Slf4jFactory[F] =
    Slf4jFactory.create[F]

  private val urlBase: String =
    s"http://$serverIPnPort/metrics/job/$metricsJob/instance/sparknow"

  private val timeoutDurationIdleConnectionTime: FiniteDuration = 5.seconds
  private val timeoutDurationIdleTimeInPool: FiniteDuration = 5.seconds
  private val timeoutDurationTimeout: FiniteDuration = 5.seconds

  private val client = EmberClientBuilder
    .default[IO]
    .withIdleConnectionTime(timeoutDurationIdleConnectionTime)
    .withIdleTimeInPool(timeoutDurationIdleTimeInPool)
    .withTimeout(timeoutDurationTimeout)
    .build

  /** Validate a string by removing invalid characters and ensuring it adheres to a specified
    * pattern.
    *
    * @param name
    *   The string to be validated.
    * @param validChars
    *   A regular expression specifying the allowed characters in the string.
    * @return
    *   A validated string, or `null` if the input string is `null`.
    */
  private def validateName(name: String, validChars: String): String = {
    Option(name)
      .map(_.replaceAll(validChars, " ").trim)
      .map { trimmedStr =>
        val resultStr =
          if (trimmedStr.nonEmpty && trimmedStr.charAt(0).isDigit) s"_$trimmedStr" else trimmedStr
        resultStr.replaceAll(" ", "_")
      }
      .orNull
  }

  /** Validate a label name ensuring it conforms to specified pattern. Allowed characters include
    * letters (both uppercase and lowercase), digits, and underscore.
    *
    * @param name
    *   The label name to validate.
    * @return
    *   A validated label name.
    */
  private def validateLabel(name: String): String = validateName(name, s"[^a-zA-Z0-9_]")

  /** Validate a metric name ensuring it conforms to specified pattern. Allowed characters include
    * letters (both uppercase and lowercase), digits, colon, and underscore.
    *
    * @param name
    *   The metric name to validate.
    * @return
    *   A validated metric name.
    */
  def validateMetric(name: String): String = validateName(name, s"[^a-zA-Z0-9_:]")

  /** Build an HTTP request for posting metrics data to a specified URL.
    *
    * @param metrics
    *   The metrics data to be posted.
    * @param metricsType
    *   The type of metrics data.
    * @param labelName
    *   The name of the label associated with the metrics data.
    * @param labelValue
    *   The value of the label associated with the metrics data.
    * @return
    *   An `IO` representing the HTTP request.
    */
  private def buildRequest(
      metrics: String,
      metricsType: String,
      labelName: String,
      labelValue: String): IO[Request[IO]] = {
    val urlType = Option(metricsType).filter(_.nonEmpty).getOrElse("NoType")
    val urlLabelName =
      Option(labelName).map(validateLabel).filter(_.nonEmpty).getOrElse("NoLabelName")
    val urlLabelValue = Option(labelValue).filter(_.nonEmpty).getOrElse("NoLabelValue")
    val urlFull = Uri.unsafeFromString(s"$urlBase/type/$urlType/$urlLabelName/$urlLabelValue")
    val entity: Entity[IO] = EntityEncoder[IO, String].toEntity(metrics)

    IO(
      Request[IO](
        method = Method.POST,
        uri = urlFull,
        headers = Headers(`Content-Type`(MediaType.text.plain)),
        entity = entity))
  }

  /** Post metrics data.
    *
    * @param metrics
    *   The metrics data to be posted.
    * @param metricsType
    *   The type of metrics data.
    * @param labelName
    *   The name of the label associated with the metrics data.
    * @param labelValue
    *   The value of the label associated with the metrics data.
    */
  def post(metrics: String, metricsType: String, labelName: String, labelValue: String): Unit = {
    buildRequest(metrics, metricsType, labelName, labelValue)
      .flatMap { request =>
        client
          .use { client =>
            client.run(request).use { response =>
              val responseCode = response.status.code
              if (responseCode != 200 && responseCode != 201 && responseCode != 202) {
                IO(logger.error(
                  s"Data sent error, url: '${request.uri}', response: $responseCode '${response.status.reason}', body: $metrics"))
              } else IO.unit
            }
          }
          .as(ExitCode.Success)
          .handleError { throwable =>
            logger.error(s"Data sent error, url: '${request.uri}', ${throwable.getMessage}")
            ExitCode.Error
          }
          .handleErrorWith { _ =>
            IO.pure(ExitCode.Error)
          }
      }
      .unsafeRunAsync(_.left) { ioRuntime }
  }

}
