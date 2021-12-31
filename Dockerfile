FROM ubuntu:20.04
MAINTAINER Jason Lowe-Power <jason@lowepower.com>

ENV SBT_VERSION 0.13.16
ENV DEBIAN_FRONTEND noninteractive 
ENV TZ America/Los_Angeles

RUN apt-get update -y

RUN \
  apt-get install -y default-jre git make autoconf g++ flex bison pkg-config libxext6 libfontconfig1 libxrender1 libxtst6 gnupg gcc-riscv64-linux-gnu g++-riscv64-linux-gnu curl

# Install sbt
RUN \
  echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
  echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
  curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import && \
  chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg && \
  apt-get update -y && \
  apt-get install sbt -y

CMD sbt -mem 2048
