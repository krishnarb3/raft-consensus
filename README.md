# Raft consensus
Raft consensus protocol and implementation in Kotlin

## Raft consensus API
Defines raft node behavior and logic

## Raft Spring implementation
Implementation of the raft consensus api using spring boot

### Build 
```bash
cd raft-consensus-impl
../gradlew clean build -x test
```

### Run
```bash
../gradlew bootRun
```

## Raft vertx implementation
Implementation of the raft consensus api using vert.x <br />
Each verticle can be spawned as a raft node <br />
Cluster management is handled by vert.x <br />

### Checkout
```bash
git checkout raft-consensus-vertx
```

### Build
```bash
cd raft-consensus-vertx-impl
../gradlew clean build -x test
```

### Run
```bash
java -jar build/libs/raft-consensus-vertx-impl-1.0-SNAPSHOT-fat.jar -cluster -Dhttp_port=8080
java -jar build/libs/raft-consensus-vertx-impl-1.0-SNAPSHOT-fat.jar -cluster -Dhttp_port=8081
java -jar build/libs/raft-consensus-vertx-impl-1.0-SNAPSHOT-fat.jar -cluster -Dhttp_port=8082
```

This creates 3 verticles which communicate using the vert.x eventbus. <br />
HTTP services are exposed for convenience.
