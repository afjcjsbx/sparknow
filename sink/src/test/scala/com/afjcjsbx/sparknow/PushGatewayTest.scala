package com.afjcjsbx.sparknow

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class PushGatewayTest extends AnyFunSuiteLike with Matchers with BeforeAndAfterAll {

  private val portNumber = 9191
  // Start WireMock server
  private val wireMockServer = new WireMockServer(
    WireMockConfiguration.wireMockConfig().port(portNumber))
  wireMockServer.start()

  test("post should send data to PushGateway server") {
    val metricsJob = "test-job"
    val metricsData = "testMetricsData"
    val metricsType = "testType"
    val labelName = "testLabelName"
    val labelValue = "testLabelValue"
    val urlBase = s"/metrics/job/" + metricsJob + s"/instance/sparknow"
    val urlFull = urlBase + s"/type/" + metricsType + s"/" + labelName + s"/" + labelValue

    // Mock response from PushGateway server
    wireMockServer.stubFor(
      post(urlPathEqualTo(urlFull))
        .willReturn(aResponse().withStatus(201)))

    val pushGateway = PushGateway(s"localhost:$portNumber", "test-job")

    // Perform POST request to PushGateway
    pushGateway.post(metricsData, metricsType, labelName, labelValue)

    Thread.sleep(1000)

    // Verify that WireMock server received the request
    wireMockServer.verify(
      postRequestedFor(urlPathEqualTo(urlFull))
        .withHeader("Content-Type", containing("text/plain"))
        .withRequestBody(containing(metricsData)))
  }

  // Stop WireMock server after tests
  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

}
