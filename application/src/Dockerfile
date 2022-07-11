FROM ubuntu:latest

EXPOSE 8080/tcp

RUN apt update \
  && apt install -y libcurl4-openssl-dev wget unzip \
  && wget http://security.ubuntu.com/ubuntu/pool/main/s/sqlite3/libsqlite3-0_3.22.0-1ubuntu0.5_amd64.deb \
  && apt install -y ./libsqlite3-0_3.22.0-1ubuntu0.5_amd64.deb \
  && ln -s /usr/lib/x86_64-linux-gnu/libsqlite3.so.0 /usr/lib/x86_64-linux-gnu/libsqlite3.so \
