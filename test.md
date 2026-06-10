# ArcusDB YCSB 테스트 가이드

이 문서는 `devbox.gcp.namsic.dev:3056`에 떠 있는 ArcusDB 서버를 대상으로 YCSB 테스트를 실행하는 방법을 정리합니다.

테스트 collection 이름은 `test`로 사용합니다.

## 1. 빌드 및 설치

```bash
./build.sh
cd ycsb-0.17.0
```

`build.sh`는 YCSB 0.17.0을 준비하고, ArcusDB binding jar와 workload 파일을 YCSB에 설치합니다.

## 2. 공통 접속 설정

아래 값은 모든 테스트 명령에 공통으로 들어갑니다.

```bash
-p table=test \
-p arcusdb.host=devbox.gcp.namsic.dev \
-p arcusdb.port=3056
```

의미는 아래와 같습니다.

| 설정             | 값                       | 설명                    |
|----------------|-------------------------|-----------------------|
| `table`        | `test`                  | ArcusDB collection 이름 |
| `arcusdb.host` | `devbox.gcp.namsic.dev` | ArcusDB 서버 host       |
| `arcusdb.port` | `3056`                  | ArcusDB 서버 port       |

## 3. 1건 확인 테스트

큰 테스트를 실행하기 전에 1건만 insert/read 해서 기본 동작을 확인합니다.

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

정상 결과 예:

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

정상 결과 예:

```text
[READ], Return=OK, 1
```

## 4. Workload C

`arcusdb-workloadc`는 run 단계에서 read 100%를 수행합니다. 현재 바인딩은 `insert`, `read`만 지원하므로 가장 먼저 확인하기 좋은 workload입니다.

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

`arcusdb-workloada`는 read 50% / insert 50% workload입니다.

YCSB 기본 workload A는 read 50% / update 50%이지만, 현재 바인딩은 `update`를 지원하지 않습니다. 그래서 ArcusDB용 workload A에서는 write 작업을 insert로 대체했습니다.

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

`arcusdb-workloadb`는 read 95% / insert 5% workload입니다.

YCSB 기본 workload B는 read 95% / update 5%이지만, 현재 바인딩은 `update`를 지원하지 않습니다. 그래서 ArcusDB용 workload B에서는 write 작업을 insert로 대체했습니다.

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

## 7. 주의사항

- collection 이름을 `test`로 쓰려면 모든 명령에 `-p table=test`를 넣어야 합니다.
- 큰 테스트 전에 1건 확인 테스트를 먼저 실행하세요.
- `NOT_IMPLEMENTED`가 나오면 현재 지원하지 않는 YCSB 작업이 workload에 포함된 것입니다.
- 현재 미지원 작업은 `update`, `scan`, `delete`입니다.
