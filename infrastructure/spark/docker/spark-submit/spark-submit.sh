#!/bin/bash

/spark/bin/spark-submit \
--class ${SPARK_APPLICATION_MAIN_CLASS} \
--master ${SPARK_MASTER_URL} \
--deploy-mode cluster \
--total-executor-cores 1 \
--jars $(echo /opt/spark-apps/*.jar | tr ' ' ',') \
 ${SPARK_SUBMIT_ARGS} \
 ${SPARK_APPLICATION_JAR_LOCATION}
