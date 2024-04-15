package com.afjcjsbx.sparknow

import org.apache.spark.scheduler._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.ui.{
  SparkListenerSQLExecutionEnd,
  SparkListenerSQLExecutionStart
}
import org.apache.spark.{SparkConf, TaskEndReason, TaskFailedReason}
import org.slf4j.{Logger, LoggerFactory}

abstract class AbstractSink(conf: SparkConf) extends SparkListener {
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  protected val appId: String =
    SparkSession.getActiveSession.map(_.sparkContext.applicationId).getOrElse("noAppId")

  private val isExtendedMode: Boolean = Utils.parseExtendedModeConfig(conf)

  /** Reports metrics to the Prometheus Push Gateway.
    *
    * @param metricsContainer
    *   The container of metrics being reported.
    */
  protected def report(metricsContainer: MetricsContainer): Unit

  /** Handles the event of a stage being submitted and reports corresponding metrics.
    *
    * @param stageSubmitted
    *   The event representing the submission of a stage.
    */
  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    val submissionTime = stageSubmitted.stageInfo.submissionTime.getOrElse(0L)
    val attemptNumber = stageSubmitted.stageInfo.attemptNumber().toLong
    val stageId = stageSubmitted.stageInfo.stageId.toLong
    val epochMillis = System.currentTimeMillis()

    val stageSubmittedMetricsContainer =
      MetricsContainer(s"stageSubmitted-$stageId-$attemptNumber")
        .add("name" -> "stages_started")
        .add("appId" -> appId)
        .add("stageId" -> stageId)
        .add("attemptNumber" -> attemptNumber)
        .add("submissionTime" -> submissionTime)
        .add("epochMillis" -> epochMillis)

    report(stageSubmittedMetricsContainer)
  }

  /** Handles the event of a stage being completed and reports corresponding metrics.
    *
    * @param stageCompleted
    *   The event representing the completion of a stage.
    */
  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    val stageId = stageCompleted.stageInfo.stageId.toLong
    val submissionTime = stageCompleted.stageInfo.submissionTime.getOrElse(0L)
    val completionTime = stageCompleted.stageInfo.completionTime.getOrElse(0L)
    val attemptNumber = stageCompleted.stageInfo.attemptNumber().toLong

    val epochMillis = System.currentTimeMillis()
    val stageEndMetricsContainer = MetricsContainer(s"stageEnd-$stageId-$attemptNumber")
      .add("name" -> "stages_ended")
      .add("appId" -> appId)
      .add("stageId" -> stageId)
      .add("attemptNumber" -> attemptNumber)
      .add("submissionTime" -> submissionTime)
      .add("completionTime" -> completionTime)
      .add("epochMillis" -> epochMillis)

    report(stageEndMetricsContainer)

    // Report stage task metric
    val taskMetrics = stageCompleted.stageInfo.taskMetrics
    val stageMetricsContainer = MetricsContainer(s"stageMetrics-$stageId-$attemptNumber")
      .add("name" -> "stage_metrics")
      .add("appId" -> appId)
      .add("stageId" -> stageId)
      .add("attemptNumber" -> attemptNumber)
      .add("submissionTime" -> submissionTime)
      .add("completionTime" -> completionTime)
      .add("failureReason" -> stageCompleted.stageInfo.failureReason.getOrElse(""))
      .add("executorRunTime" -> taskMetrics.executorRunTime)
      .add("executorCpuTime" -> taskMetrics.executorRunTime)
      .add("executorDeserializeCpuTime" -> taskMetrics.executorDeserializeCpuTime)
      .add("executorDeserializeTime" -> taskMetrics.executorDeserializeTime)
      .add("jvmGCTime" -> taskMetrics.jvmGCTime)
      .add("memoryBytesSpilled" -> taskMetrics.memoryBytesSpilled)
      .add("peakExecutionMemory" -> taskMetrics.peakExecutionMemory)
      .add("resultSerializationTime" -> taskMetrics.resultSerializationTime)
      .add("resultSize" -> taskMetrics.resultSize)
      .add("bytesRead" -> taskMetrics.inputMetrics.bytesRead)
      .add("recordsRead" -> taskMetrics.inputMetrics.recordsRead)
      .add("bytesWritten" -> taskMetrics.outputMetrics.bytesWritten)
      .add("recordsWritten" -> taskMetrics.outputMetrics.recordsWritten)
      .add("shuffleTotalBytesRead" -> taskMetrics.shuffleReadMetrics.totalBytesRead)
      .add("shuffleRemoteBytesRead" -> taskMetrics.shuffleReadMetrics.remoteBytesRead)
      .add("shuffleRemoteBytesReadToDisk" -> taskMetrics.shuffleReadMetrics.remoteBytesReadToDisk)
      .add("shuffleLocalBytesRead" -> taskMetrics.shuffleReadMetrics.localBytesRead)
      .add("shuffleTotalBlocksFetched" -> taskMetrics.shuffleReadMetrics.totalBlocksFetched)
      .add("shuffleLocalBlocksFetched" -> taskMetrics.shuffleReadMetrics.localBlocksFetched)
      .add("shuffleRemoteBlocksFetched" -> taskMetrics.shuffleReadMetrics.remoteBlocksFetched)
      .add("shuffleRecordsRead" -> taskMetrics.shuffleReadMetrics.recordsRead)
      .add("shuffleFetchWaitTime" -> taskMetrics.shuffleReadMetrics.fetchWaitTime)
      .add("shuffleBytesWritten" -> taskMetrics.shuffleWriteMetrics.bytesWritten)
      .add("shuffleRecordsWritten" -> taskMetrics.shuffleWriteMetrics.recordsWritten)
      .add("shuffleWriteTime" -> taskMetrics.shuffleWriteMetrics.writeTime)
      .add("numTasks" -> stageCompleted.stageInfo.numTasks)
      .add("epochMillis" -> epochMillis)

    report(stageMetricsContainer)
  }

  /** Handles the event of an executor being excluded and reports corresponding metrics.
    *
    * This method is invoked when an executor is excluded from the Spark application due to
    * failures. It collects relevant metrics, such as the timestamp of exclusion, executor ID, and
    * number of task failures.
    *
    * @param executorExcluded
    *   The event representing the exclusion of an executor.
    */
  override def onExecutorExcluded(executorExcluded: SparkListenerExecutorExcluded): Unit = {
    val time = executorExcluded.time
    val executorId = executorExcluded.executorId
    val taskFailures = executorExcluded.taskFailures
    val epochMillis = System.currentTimeMillis()

    val executorExcludedMetricsContainer = MetricsContainer(s"executorExcluded-$executorId")
      .add("name" -> "executor_excluded")
      .add("appId" -> appId)
      .add("time" -> time)
      .add("executorId" -> executorId)
      .add("taskFailures" -> taskFailures)
      .add("epochMillis" -> epochMillis)

    report(executorExcludedMetricsContainer)
  }

  /** Handles events related to the addition of new executors to the Spark cluster. Collects
    * corresponding metrics and reports them.
    *
    * @param executorAdded
    *   . The Spark event related to the addition of an executor.
    */
  override def onExecutorAdded(executorAdded: SparkListenerExecutorAdded): Unit = {
    val time = executorAdded.time
    val executorId = executorAdded.executorId
    val executorInfo = executorAdded.executorInfo
    val executorHost = executorInfo.executorHost
    val totalCores = executorInfo.totalCores
    val epochMillis = System.currentTimeMillis()

    val executorAddedMetricsContainer = MetricsContainer(s"executorAdded-$executorId")
      .add("name" -> "executor_added")
      .add("appId" -> appId)
      .add("time" -> time)
      .add("executorId" -> executorId)
      .add("executorHost" -> executorHost)
      .add("totalCores" -> totalCores)
      .add("epochMillis" -> epochMillis)

    report(executorAddedMetricsContainer)
  }

  /** Handles events related to the removal of executors from the Spark cluster. Collects
    * corresponding metrics and reports them.
    *
    * @param executorRemoved
    *   . The Spark event related to the removal of an executor.
    */
  override def onExecutorRemoved(executorRemoved: SparkListenerExecutorRemoved): Unit = {
    val time = executorRemoved.time
    val executorId = executorRemoved.executorId
    val reason = executorRemoved.reason
    val epochMillis = System.currentTimeMillis()

    val executorRemovedMetricsContainer = MetricsContainer(s"executorRemoved-$executorId")
      .add("name" -> "executor_removed")
      .add("appId" -> appId)
      .add("time" -> time)
      .add("executorId" -> executorId)
      .add("reason" -> reason)
      .add("epochMillis" -> epochMillis)

    report(executorRemovedMetricsContainer)
  }

  /** Handles miscellaneous Spark events, such as SQL execution start/end, and reports
    * corresponding metrics.
    *
    * @param event
    *   The Spark event.
    */
  override def onOtherEvent(event: SparkListenerEvent): Unit = {
    val epochMillis = System.currentTimeMillis()
    event match {
      case e: SparkListenerSQLExecutionStart =>
        val startTime = e.time
        val queryId = e.executionId
        val description = e.description

        val queryStartMetricsContainer = MetricsContainer(s"queryStart-$queryId")
          .add("name" -> "queries_started")
          .add("appId" -> appId)
          .add("description" -> description)
          .add("queryId" -> queryId)
          .add("startTime" -> startTime)
          .add("epochMillis" -> epochMillis)

        report(queryStartMetricsContainer)
      case e: SparkListenerSQLExecutionEnd =>
        val endTime = e.time
        val queryId = e.executionId

        val queryEndMetricsContainer = MetricsContainer(s"queryEnd-$queryId")
          .add("name" -> "queries_ended")
          .add("appId" -> appId)
          .add("queryId" -> queryId)
          .add("endTime" -> endTime)
          .add("epochMillis" -> epochMillis)

        report(queryEndMetricsContainer)
      case _ =>
    }
  }

  /** Handles the event of a Spark job starting and reports corresponding metrics.
    *
    * @param jobStart
    *   The event representing the start of a Spark job.
    */
  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    val startTime = jobStart.time
    val jobId = jobStart.jobId.toLong
    val epochMillis = System.currentTimeMillis()

    val jobStartMetricsContainer = MetricsContainer(s"jobStart-$jobId")
      .add("name" -> "jobs_started")
      .add("appId" -> appId)
      .add("jobId" -> jobId)
      .add("startTime" -> startTime)
      .add("epochMillis" -> epochMillis)

    report(jobStartMetricsContainer)
  }

  /** Handles the event of a Spark job ending and reports corresponding metrics.
    *
    * @param jobEnd
    *   The event representing the end of a Spark job.
    */
  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    val completionTime = jobEnd.time
    val jobId = jobEnd.jobId.toLong
    val epochMillis = System.currentTimeMillis()

    val jobResult: String = jobEnd.jobResult match {
      case JobSucceeded => "Succeeded"
      case _ @e =>
        val errorMessage = e.toString
        s"Failed: $errorMessage"
    }

    val jobEndMetricsContainer = MetricsContainer(s"jobEnd-$jobId")
      .add("name" -> "jobs_ended")
      .add("appId" -> appId)
      .add("jobId" -> jobId)
      .add("completionTime" -> completionTime)
      .add("jobResult" -> jobResult)
      .add("epochMillis" -> epochMillis)

    report(jobEndMetricsContainer)
  }

  override def onTaskStart(taskStart: SparkListenerTaskStart): Unit =
    checkExtendedMode {
      val taskInfo = taskStart.taskInfo
      val epochMillis = System.currentTimeMillis()

      val taskStartMetricsContainer =
        MetricsContainer(s"taskStart-${taskInfo.taskId}-${taskInfo.attemptNumber}")
          .add("name" -> "tasks_started")
          .add("appId" -> appId)
          .add("taskId" -> taskInfo.taskId)
          .add("attemptNumber" -> taskInfo.attemptNumber)
          .add("stageId" -> taskStart.stageId)
          .add("launchTime" -> taskInfo.launchTime)
          .add("epochMillis" -> epochMillis)

      report(taskStartMetricsContainer)
    }

  /** Handles events related to the completion of a task in the Spark application. Collects
    * corresponding metrics and reports them.
    *
    * @param taskEnd
    *   . The Spark event related to the end of a task.
    */
  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit =
    checkExtendedMode {
      val taskInfo = taskEnd.taskInfo
      val taskmetrics = taskEnd.taskMetrics
      val epochMillis = System.currentTimeMillis()

      val taskEndMetricsContainer =
        MetricsContainer(s"taskEnd-${taskInfo.taskId}-${taskInfo.attemptNumber}")
          .add("name" -> "tasks_started")
          .add("appId" -> appId)
          .add("taskId" -> taskInfo.taskId)
          .add("attemptNumber" -> taskInfo.attemptNumber)
          .add("stageId" -> taskEnd.stageId)
          .add("launchTime" -> taskInfo.launchTime)
          .add("finishTime" -> taskInfo.finishTime)
          .add("epochMillis" -> epochMillis)

      report(taskEndMetricsContainer)

      val reason: TaskEndReason = taskEnd.reason
      val failureReason: String = reason match {
        case _: TaskFailedReason =>
          reason.asInstanceOf[TaskFailedReason].toErrorString
        case _ => ""
      }

      val taskMetricsContainer =
        MetricsContainer(s"taskMetrics-${taskInfo.taskId}-${taskInfo.attemptNumber}")
          .add("name" -> "task_metrics")
          .add("appId" -> appId)
          .add("taskId" -> taskInfo.taskId)
          .add("attemptNumber" -> taskInfo.attemptNumber)
          .add("stageId" -> taskEnd.stageId)
          .add("launchTime" -> taskInfo.launchTime)
          .add("finishTime" -> taskInfo.finishTime)
          .add("status" -> taskInfo.status)
          .add("failed" -> taskInfo.failed)
          .add("failureReason" -> failureReason)
          .add("speculative" -> taskInfo.speculative)
          .add("killed" -> taskInfo.killed)
          .add("finished" -> taskInfo.finished)
          .add("executorId" -> taskInfo.executorId)
          .add("duration" -> taskInfo.duration)
          .add("successful" -> taskInfo.successful)
          .add("host" -> taskInfo.host)
          .add("taskLocality" -> Utils.encodeTaskLocality(taskInfo.taskLocality))
          .add("executorRunTime" -> taskmetrics.executorRunTime)
          .add("executorCpuTime" -> taskmetrics.executorCpuTime)
          .add("executorDeserializeCpuTime" -> taskmetrics.executorDeserializeCpuTime)
          .add("executorDeserializeTime" -> taskmetrics.executorDeserializeTime)
          .add("jvmGCTime" -> taskmetrics.jvmGCTime)
          .add("memoryBytesSpilled" -> taskmetrics.memoryBytesSpilled)
          .add("peakExecutionMemory" -> taskmetrics.peakExecutionMemory)
          .add("resultSerializationTime" -> taskmetrics.resultSerializationTime)
          .add("resultSize" -> taskmetrics.resultSize)
          .add("bytesRead" -> taskmetrics.inputMetrics.bytesRead)
          .add("recordsRead" -> taskmetrics.inputMetrics.recordsRead)
          .add("bytesWritten" -> taskmetrics.outputMetrics.bytesWritten)
          .add("recordsWritten" -> taskmetrics.outputMetrics.recordsWritten)
          .add("shuffleTotalBytesRead" -> taskmetrics.shuffleReadMetrics.totalBytesRead)
          .add("shuffleRemoteBytesRead" -> taskmetrics.shuffleReadMetrics.remoteBytesRead)
          .add("shuffleLocalBytesRead" -> taskmetrics.shuffleReadMetrics.localBytesRead)
          .add("shuffleTotalBlocksFetched" -> taskmetrics.shuffleReadMetrics.totalBlocksFetched)
          .add("shuffleLocalBlocksFetched" -> taskmetrics.shuffleReadMetrics.localBlocksFetched)
          .add("shuffleRemoteBlocksFetched" -> taskmetrics.shuffleReadMetrics.remoteBlocksFetched)
          .add("shuffleRecordsRead" -> taskmetrics.shuffleReadMetrics.recordsRead)
          .add("remoteBytesReadToDisk" -> taskmetrics.shuffleReadMetrics.remoteBytesReadToDisk)
          .add("shuffleFetchWaitTime" -> taskmetrics.shuffleReadMetrics.fetchWaitTime)
          .add("shuffleBytesWritten" -> taskmetrics.shuffleWriteMetrics.bytesWritten)
          .add("shuffleRecordsWritten" -> taskmetrics.shuffleWriteMetrics.recordsWritten)
          .add("shuffleWriteTime" -> taskmetrics.shuffleWriteMetrics.writeTime)
          .add("epochMillis" -> epochMillis)

      report(taskMetricsContainer)
    }

  /** Handles the event of a Spark application starting and logs relevant information.
    *
    * @param applicationStart
    *   The event representing the start of a Spark application.
    */
  override def onApplicationStart(applicationStart: SparkListenerApplicationStart): Unit =
    logger.info(
      s"Spark application started, " +
        s"Application Name: ${applicationStart.appName}, " +
        s"Application ID: ${applicationStart.appId.getOrElse("noAppId")}, " +
        s"Time: ${applicationStart.time}, " +
        s"Spark User: ${applicationStart.sparkUser}, " +
        s"App Attempt ID: ${applicationStart.appAttemptId.getOrElse("N/A")}")

  /** Handles the event of a Spark application ending and logs relevant information.
    *
    * @param applicationEnd
    *   The event representing the end of a Spark application.
    */
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit =
    logger.info(s"Spark application ended, timestamp = ${applicationEnd.time}")

  private def checkExtendedMode[T](f: => T): Unit = {
    if (isExtendedMode) f
  }
}
