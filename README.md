# RaftStore

## Example Usage

### Build JAR File

```bash
./gradlew bootJar
```

### Configure Group

```bash
export GROUP_ID=demoRaftGroup123
export PEERS_IDS=n1,n2,n3
export PEERS_ADDRS=localhost:6001,localhost:6002,localhost:6003
```

### Run Nodes

```bash
DATA_DIR=n1 PEER_ID=n1 SERVER_PORT=8081 java -jar build/libs/raftstore-*.jar &
DATA_DIR=n2 PEER_ID=n2 SERVER_PORT=8082 java -jar build/libs/raftstore-*.jar &
DATA_DIR=n3 PEER_ID=n3 SERVER_PORT=8083 java -jar build/libs/raftstore-*.jar &
```

### Set Key-Value Pair

```bash
curl -X POST localhost:8081 \
  -H 'Content-Type: application/json' \
  -d '{"op": "set", "key": "foo", "val": "bar"}'
```

### Get Key-Value Pair

```bash
curl -X POST localhost:8082 \
  -H 'Content-Type: application/json' \
  -d '{"op": "get", "key": "foo"}'
```
