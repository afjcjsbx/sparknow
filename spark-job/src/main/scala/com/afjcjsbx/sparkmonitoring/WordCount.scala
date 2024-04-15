package com.afjcjsbx.sparkmonitoring

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.logging.log4j.{LogManager, Logger}


object WordCount {
  private val logger = LogManager.getLogger(WordCount.getClass)

  def main(args: Array[String]): Unit = {
    logger.info("Start processing")

    val spark = SparkSession
      .builder()
      .appName("WordCount")
      // .master("local")
      .getOrCreate()
    spark.sparkContext.setLogLevel("INFO")

    import spark.implicits._

    spark.sparkContext.addFile("opt/spark-data/treasure_island.txt")

    val linesDF = spark.sparkContext.textFile("opt/spark-data/treasure_island.txt").toDF("value")

    val wordsDF = linesDF
      .select(explode(split($"value", "\\s+")).as("word"))
      .filter(length(col("word")) > 1)
      .select(lower(col("word")).as("word"))

    val wordCountsDF = wordsDF
      .groupBy("word")
      .agg(count("word").alias("count"))
      .orderBy(col("count").desc_nulls_last)

    wordCountsDF.show(false)

    wordCountsDF
      .coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .format("json")
      .save("opt/spark-data/word_count")

    spark.stop()
  }
}
