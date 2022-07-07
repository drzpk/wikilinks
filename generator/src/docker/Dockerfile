FROM public.ecr.aws/amazoncorretto/amazoncorretto:11

RUN yum install -y aws-cli bc
COPY downloader.sh /
RUN chmod +x /downloader.sh

ENTRYPOINT ["/downloader.sh"]
