# ArcusDB YCSB Test Guide

## 1. Build

```bash
./build.sh
cd ycsb-0.17.0
```

## 2. Connection

Use this ArcusDB server:

```text
host: devbox.gcp.namsic.dev
port: 3056
table(collection): test
```

Common parameters:

```bash
-p table=test \
-p arcusdb.host=devbox.gcp.namsic.dev \
-p arcusdb.port=3056
```

## 3. Smoke Test

Run this first. Workload C is read-only during the run phase, so it is the safest check for the current implementation.

```bash
./bin/ycsb-arcusdb load \
  -P workloads/arcusdb-workloadc \
  -p table=test \
  -p recordcount=1 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -p arcusdb.timeoutMillis=3000 \
  -s
```

Expected result:

```text
[INSERT], Return=OK, 1
```

```bash
./bin/ycsb-arcusdb run \
  -P workloads/arcusdb-workloadc \
  -p table=test \
  -p recordcount=1 \
  -p operationcount=1 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -p arcusdb.timeoutMillis=3000 \
  -s
```

Expected result:

```text
[READ], Return=OK, 1
```

## 4. Workload C

Read 100%.

```bash
./bin/ycsb-arcusdb load \
  -P workloads/arcusdb-workloadc \
  -p table=test \
  -p recordcount=1500 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -s
```

```bash
./bin/ycsb-arcusdb run \
  -P workloads/arcusdb-workloadc \
  -p table=test \
  -p recordcount=1500 \
  -p operationcount=10000 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -s
```

## 5. Workload A

Read 50% / insert 50%.

The standard YCSB workload A is read 50% / update 50%, but update is not implemented in this binding yet.

```bash
./bin/ycsb-arcusdb load \
  -P workloads/arcusdb-workloada \
  -p table=test \
  -p recordcount=1500 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -s
```

```bash
./bin/ycsb-arcusdb run \
  -P workloads/arcusdb-workloada \
  -p table=test \
  -p recordcount=1500 \
  -p operationcount=5000000 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -threads 128 \
  -s
```

## 6. Workload B

Read 95% / insert 5%.

The standard YCSB workload B is read 95% / update 5%, but update is not implemented in this binding yet.

```bash
./bin/ycsb-arcusdb load \
  -P workloads/arcusdb-workloadb \
  -p table=test \
  -p recordcount=1500 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -s
```

```bash
./bin/ycsb-arcusdb run \
  -P workloads/arcusdb-workloadb \
  -p table=test \
  -p recordcount=1500 \
  -p operationcount=5000000 \
  -p arcusdb.host=devbox.gcp.namsic.dev \
  -p arcusdb.port=3056 \
  -threads 128 \
  -s
```

## 7. Notes

- Always include `-p table=test` if the target collection is `test`.
- Start with the smoke test before running large operation counts.
- If `NOT_IMPLEMENTED` appears, the workload includes an unsupported operation.
- Current unsupported operations are `update`, `scan`, and `delete`.
