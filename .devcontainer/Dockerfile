# ─────────────────────────────────────────────────────────────────────────────
# Effortless Lite - NeoForge Mod DevContainer
#
# ベースイメージ: Eclipse Temurin 21 (Ubuntu 22.04 ベース)
# Amazon Corretto は curl-minimal との競合問題があるため変更
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy

# ─────────────────────────────────────────────────────────────────────────────
# 開発ツールのインストール
# ─────────────────────────────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
        git \
        curl \
        wget \
        unzip \
        zip \
        findutils \
        make \
        sudo \
        language-pack-ja \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ─────────────────────────────────────────────────────────────────────────────
# Gradle 8.8 のインストール
# ─────────────────────────────────────────────────────────────────────────────
ENV GRADLE_VERSION=8.8
RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
        -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt/ && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

# ─────────────────────────────────────────────────────────────────────────────
# 開発用ユーザーの作成
# ─────────────────────────────────────────────────────────────────────────────
ARG USERNAME=mcdev
ARG USER_UID=1000
ARG USER_GID=1000

RUN groupadd --gid ${USER_GID} ${USERNAME} && \
    useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME} && \
    echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# ─────────────────────────────────────────────────────────────────────────────
# Gradle キャッシュディレクトリ
# ─────────────────────────────────────────────────────────────────────────────
ENV GRADLE_USER_HOME=/home/${USERNAME}/.gradle
RUN mkdir -p ${GRADLE_USER_HOME} && \
    chown -R ${USERNAME}:${USERNAME} ${GRADLE_USER_HOME}

# ─────────────────────────────────────────────────────────────────────────────
# 環境変数
# ─────────────────────────────────────────────────────────────────────────────
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV LANG=ja_JP.UTF-8

WORKDIR /workspace
RUN chown ${USERNAME}:${USERNAME} /workspace

USER ${USERNAME}

RUN echo "=== Build Environment ===" && \
    java -version && \
    gradle --version && \
    git --version
