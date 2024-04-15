FROM spark351-scala213-base:latest

COPY spark-submit.sh /

ENV SPARK_MASTER_URL="spark://spark-master:7077"
ENV SPARK_SUBMIT_ARGS=""
ENV SPARK_APPLICATION_ARGS=""

CMD ["/bin/bash", "/spark-submit.sh"]
