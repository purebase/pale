FROM navikt/java:8
COPY build/install/* /app
ENV JAVA_OPTS="'-Dlogback.configurationFile=logback-remote.xml'"
ENV JOURNALBEHANDLING_ENDPOINTURL="https://wasapp-t1.adeo.no/joark/Journalbehandling"
ENTRYPOINT ["/app/bin/legeerklaering"]
