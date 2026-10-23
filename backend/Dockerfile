# Build context proje kök dizini olmalı: docker build -f backend/Dockerfile .
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
RUN ./mvnw -pl backend -am dependency:go-offline -B
COPY common/src common/src
COPY backend/src backend/src

RUN ./mvnw -pl backend -am clean package -DskipTests -B && \
    cd backend && java -Djarmode=layertools -jar target/backend-*.jar extract

FROM amazoncorretto:21-alpine

RUN addgroup -S cpgroup && adduser -S cpadmin -G cpgroup
USER cpadmin

WORKDIR /app
COPY --from=builder /app/backend/spring-boot-loader/ ./
COPY --from=builder /app/backend/dependencies/ ./
COPY --from=builder /app/backend/snapshot-dependencies/ ./
COPY --from=builder /app/backend/application/ ./

EXPOSE 8100
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
