package com.afjcjsbx.sparknow

import org.apache.spark.SparkConf
import org.scalatest.funsuite.AnyFunSuiteLike
import org.slf4j.{Logger, LoggerFactory}

class UtilsTest extends AnyFunSuiteLike {

  test("testParseExtendedModeConfig false") {
    val conf = new SparkConf()
      .setAppName("Simple Application")
      .setMaster("local")
    val extendedMode = Utils.parseExtendedModeConfig(conf)
    assert(extendedMode === false)
  }

  test("testParseExtendedModeConfig true") {
    val logger: Logger = LoggerFactory.getLogger(getClass.getName)

    val conf = new SparkConf()
      .setAppName("Simple Application")
      .setMaster("local")
      .set("spark.sparknow.extendedmode", "true")
    val extendedMode = Utils.parseExtendedModeConfig(conf)
    assert(extendedMode === true)
  }
}
