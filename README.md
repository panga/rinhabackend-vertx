## Rinha de Backend 2023 Q3 - Java + Vert.x

This application was built as a solution for Rinha de Backend 2023 Q3 challenge [instructions](https://github.com/zanfranceschi/rinha-de-backend-2023-q3/blob/main/INSTRUCOES.md).

Read my post [how to use Java superpowers to beat Rust in a backend challenge](https://medium.com/@leonardopanga/how-to-use-java-superpowers-to-beat-rust-in-a-backend-challenge-15fc219f776d).

### Solution

- Default PostgreSQL configuration.
- Standard Nginx proxy with keep alive enabled.
- No Redis/or caching.
- No queue and no SQL batching.
- Vert.x reactive framework with default settings.
- Native image build with GraalVM on Java 20.

### Run application using downloaded image

```bash
docker-compose up
```

### Run application using local build (Java)

```bash
./mvn package
docker-compose -f docker-compose-local.yml up --build
```

### Run application using local build (Native)

```bash
docker-compose -f docker-compose-native.yml up --build
```

### Run stress test

```bash
curl -o gatling.zip https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.9.5/gatling-charts-highcharts-bundle-3.9.5-bundle.zip
unzip gatling.zip
mkdir $HOME/gatling
sudo mv gatling-charts-highcharts-bundle-3.9.5-bundle $HOME/gatling/3.9.5

mkdir $HOME/projects
cd $HOME/projects
git clone https://github.com/zanfranceschi/rinha-de-backend-2023-q3.git
cd rinha-de-backend-2023-q3/stress-test
./run-test.sh
```
