FROM adoptopenjdk/openjdk11
LABEL author="afjcjsbx" email="afjcjsbx@gmail.com"
LABEL version="0.1"

ENV DAEMON_RUN=true
ENV SPARK_VERSION=3.5.1
ENV HADOOP_VERSION=3
ENV SCALA_VERSION_MAJOR=2.13
ENV SCALA_VERSION=2.13.10
ENV SCALA_HOME=/usr/share/scala
ENV SPARK_HOME=/spark

# Installing dependencies
RUN apt-get update && \
    apt-get install -y curl vim wget software-properties-common ssh net-tools ca-certificates jq dbus-x11 && \
    echo exit 0 > /usr/sbin/policy-rc.d && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Installation of Scala
RUN cd "/tmp" && \
    wget --no-verbose "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    rm -rf "/tmp/"* && \
    rm -f "/tmp/scala-${SCALA_VERSION}.tgz"

# Installation of sbt
RUN export PATH="/usr/local/sbt/bin:$PATH" && \
    apt update && \
    apt install -y ca-certificates wget tar && \
    mkdir -p "/usr/local/sbt" && \
    wget -qO - --no-check-certificate "https://github.com/sbt/sbt/releases/download/v1.7.2/sbt-1.7.2.tgz" | tar xz -C /usr/local/sbt --strip-components=1 && \
    sbt -Dsbt.rootdir=true sbtVersion && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Installing dependencies for PySpark
RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-numpy python3-matplotlib python3-scipy python3-pandas python3-simpy && \
    update-alternatives --install "/usr/bin/python" "python" "$(which python3)" 1 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Downloading and installing Apache Spark
RUN wget --no-verbose http://apache.mirror.iphh.net/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}-scala${SCALA_VERSION_MAJOR}.tgz && \
    tar -xvzf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}-scala${SCALA_VERSION_MAJOR}.tgz && \
    mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}-scala${SCALA_VERSION_MAJOR} spark && \
    rm spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}-scala${SCALA_VERSION_MAJOR}.tgz

# Adding a RUN command to set permissions on the log and work directory.
RUN mkdir -p /spark/logs && \
    chmod -R 777 /spark/logs
RUN mkdir -p /spark/work && \
    chmod -R 777 /spark/work

# Adding an unprivileged user
USER nobody
