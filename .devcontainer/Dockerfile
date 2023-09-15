FROM mcr.microsoft.com/devcontainers/base:bullseye

# Prepare apt
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive

# Install Go
ENV PATH="/usr/local/go/bin:$PATH"
RUN export GO_VERSION="1.21.1" \
    && wget "https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz" \
    && tar -C /usr/local -xzf "go${GO_VERSION}.linux-amd64.tar.gz" \
    && rm "go${GO_VERSION}.linux-amd64.tar.gz"

# Install Java
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
RUN apt-get -y install --no-install-recommends openjdk-17-jdk-headless

# Install MVN
ENV M2_REPO=/tmp/.mvn/repo
RUN export mvn_version=$(curl --head --silent --write-out "%{redirect_url}" --output /dev/null https://maven.apache.org/ref/current | grep --perl-regexp --only-matching '\d\.\d\.\d') \
    && curl --silent --insecure "https://downloads.apache.org/maven/maven-3/${mvn_version}/binaries/apache-maven-${mvn_version}-bin.tar.gz" | tar --gzip --extract --directory=/opt \
    && ln -s /opt/apache-maven-${mvn_version}/bin/mvn /usr/bin/mvn
