# Build context proje kök dizini olmalı: docker build -f external-sim/Dockerfile .
FROM amazoncorretto:21-alpine AS builder

# Apple M4'te JDK 21 SVE nedeniyle SIGILL veriyor, x86'da bu flag görmezden gelinir
ENV JAVA_TOOL_OPTIONS="-XX:+IgnoreUnrecognizedVMOptions -XX:UseSVE=0"
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY common/pom.xml common/
COPY backend/pom.xml backend/
COPY external-sim/pom.xml external-sim/
# Sadece bağımlılıkları indir (kaynak kodu henüz kopyalanmadığı için cache'lenecek)
RUN ./mvnw -pl external-sim -am dependency:go-offline -B
COPY common/src common/src
COPY external-sim/src external-sim/src

RUN ./mvnw -pl external-sim -am clean package -DskipTests -B && \
    cd external-sim && java -Djarmode=layertools -jar target/external-sim-*.jar extract

FROM amazoncorretto:21-alpine

RUN addgroup -S cpgroup && adduser -S cpadmin -G cpgroup
USER cpadmin

WORKDIR /app
COPY --from=builder /app/external-sim/spring-boot-loader/ ./
COPY --from=builder /app/external-sim/dependencies/ ./
COPY --from=builder /app/external-sim/snapshot-dependencies/ ./
COPY --from=builder /app/external-sim/application/ ./

EXPOSE 8091
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
