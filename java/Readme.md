## Build and execute

Compile and package:

```
mvn clean package
```

Execute:

```
java -jar ./target/toml2json-999-SNAPSHOT.jar
```

Native image:

```
mvn -Pnative package
```

Execute:

```
./target/toml2json
```

To produce a flamegraph:

```
mvn clean install
cd perf-test
./bench.sh
```

open profile.html in a browser.


#### Rough results on my noisy machine

```
time python convert.py
{"package": [{"name"
88.96660804748535 ms
python convert.py  0.14s user 0.05s system 100% cpu 0.187 total
```

```
time ./wasm-repro
2025/09/19 12:49:23 instantiate 81.412909ms
2025/09/19 12:49:23 {"package":[{"name":...
2025/09/19 12:49:23 convert time 51.923192ms
./wasm-repro  0.13s user 0.04s system 118% cpu 0.141 total
```

Java:

Temurin 21 JVM mode, build time compiler:

```
time java -jar ./java/target/toml2json-999-SNAPSHOT.jar
instantiate 62
{"package":[{"name":...
convert time 395
java -jar ./java/target/toml2json-999-SNAPSHOT.jar  1.20s user 0.08s system 247% cpu 0.516 total
```

Temurin 21 JVM mode, runtime compiler:

```
time java -jar ./java/target/toml2json-999-SNAPSHOT.jar
instantiate 453
{"package":[{"name":...
convert time 396
java -jar ./java/target/toml2json-999-SNAPSHOT.jar  3.79s user 0.16s system 431% cpu 0.915 total
```

Temurin 21 JVM Mode, interpreter:

```
time java -jar ./java/target/toml2json-999-SNAPSHOT.jar
instantiate 150
{"package":[{"name":...
convert time 4152
java -jar ./java/target/toml2json-999-SNAPSHOT.jar  5.60s user 0.31s system 134% cpu 4.402 total
```

Graal 25 native image, build time compiler:

```
time ./java/target/toml2json
instantiate 2
{"package":[{"name":...
convert time 159
./java/target/toml2json  0.14s user 0.03s system 99% cpu 0.166 total
```

Graal 25 native image, interpreter:

```
time ./java/target/toml2json
instantiate 25
{"package":[{"name":...
convert time 4321
./java/target/toml2json  4.28s user 0.06s system 99% cpu 4.356 total
```
