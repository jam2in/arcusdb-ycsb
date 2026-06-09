# ArcusDB YCSB Binding

이 프로젝트는 ArcusDB 서버를 YCSB로 테스트하기 위한 Java 클라이언트 바인딩입니다.

YCSB는 데이터베이스 서버를 직접 아는 도구가 아닙니다. YCSB가 `insert`, `read` 같은 작업을 호출하면, 이 프로젝트의 `ArcusYcsbClient`가 그
호출을 ArcusDB TCP/protobuf 요청으로 바꿔서 서버에 보냅니다.

즉, 이 저장소는 ArcusDB 서버가 아니라 YCSB와 ArcusDB 서버 사이에 들어가는 어댑터입니다.

## 현재 지원 범위

현재 YCSB 작업 중 아래만 구현되어 있습니다.

| YCSB 작업  | ArcusDB 요청                     | 상태  |
|----------|--------------------------------|-----|
| `insert` | `InsertRequest`                | 지원  |
| `read`   | `_id == key` 조건의 `FindRequest` | 지원  |
| `scan`   | -                              | 미지원 |
| `update` | -                              | 미지원 |
| `delete` | -                              | 미지원 |

그래서 실행 단계에서는 이 저장소가 제공하는 `arcusdb-workloadc` workload를 먼저 사용하는 것을 권장합니다.

YCSB 기본 `workloada`, `workloadb`, `workloadf`처럼 update가 섞인 workload는 현재 구현으로는 정상 성능 측정 대상이 아닙니다. 대신 이 저장소의
`arcusdb-workloada`, `arcusdb-workloadb`, `arcusdb-workloadc`를 사용하세요. load 단계는 어떤 workload 파일을 쓰더라도 insert를 수행하므로 사용할 수 있습니다.

## 준비물

테스트를 실행할 머신에 아래가 필요합니다.

- JDK 17
- Maven 3.x
- YCSB 0.17.0 압축 해제본. 없으면 `build.sh`가 다운로드할 수 있습니다.
- 실행 중인 ArcusDB 서버

YCSB 0.17.0은 예를 들어 아래처럼 받을 수 있습니다.

```bash
curl -LO https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz
tar xzf ycsb-0.17.0.tar.gz
```

## 빌드와 YCSB 설치

직접 Maven 명령을 외울 필요 없이 `build.sh`만 실행하면 됩니다.

기본 사용법은 아래 한 줄입니다.

```bash
./build.sh
```

`./ycsb-0.17.0` 디렉터리가 없으면 스크립트가 YCSB 0.17.0을 다운로드하고 압축을 풉니다.

이미 받아둔 YCSB가 다른 위치에 있다면 경로를 넘기면 됩니다.

```bash
./build.sh /path/to/ycsb-0.17.0
```

또는 환경 변수로 YCSB 위치를 넘길 수도 있습니다.

```bash
YCSB_HOME=/path/to/ycsb-0.17.0 ./build.sh
```

스크립트가 하는 일은 다섯 가지입니다.

1. `arcusdb` 모듈을 빌드해서 `arcusdb-ycsb-binding.jar`를 만듭니다.
2. 생성된 jar를 YCSB의 `arcusdb-binding/lib` 디렉터리에 복사합니다.
3. YCSB의 `bin/bindings.properties`에 `arcusdb` binding 이름을 등록합니다.
4. `scripts/workloads`의 ArcusDB 전용 workload를 YCSB의 `workloads` 디렉터리에 복사합니다.
5. YCSB 안에 `bin/ycsb-arcusdb` 실행 스크립트를 만들어 줍니다.

기존 YCSB `lib` 전체를 비우지 않습니다. 다른 binding이나 YCSB 기본 jar를 건드리지 않기 위해 ArcusDB 전용 binding 디렉터리만 사용합니다.

설치가 끝나면 `bin/ycsb-arcusdb`가 아래 DB 클래스를 자동으로 사용합니다.

```text
com.jam2in.arcusdb.ycsb.ArcusYcsbClient
```

## 실행 예시

아래 예시는 ArcusDB 서버가 `127.0.0.1:17191`에서 떠 있다고 가정합니다.

먼저 데이터를 넣습니다. YCSB에서는 이 단계를 `load`라고 부릅니다.

```bash
cd ycsb-0.17.0

./bin/ycsb-arcusdb load \
  -P workloads/arcusdb-workloadc \
  -p table=usertable \
  -p recordcount=10000 \
  -p arcusdb.host=127.0.0.1 \
  -p arcusdb.port=17191 \
  -s
```

그 다음 읽기 성능을 측정합니다. YCSB에서는 이 단계를 `run`이라고 부릅니다.

```bash
./bin/ycsb-arcusdb run \
  -P workloads/arcusdb-workloadc \
  -p table=usertable \
  -p recordcount=10000 \
  -p operationcount=10000 \
  -p arcusdb.host=127.0.0.1 \
  -p arcusdb.port=17191 \
  -s
```

`bin/ycsb-arcusdb`는 내부적으로 YCSB의 Java client를 실행하면서 `ArcusYcsbClient`를 DB 클래스로 지정합니다. 그래서 테스트를 실행하는
팀은 `-db` 값을 직접 외우지 않아도 됩니다.

원래 YCSB 명령을 그대로 쓰고 싶다면 아래처럼 실행할 수도 있습니다.

```bash
./bin/ycsb.sh load arcusdb \
  -P workloads/arcusdb-workloadc \
  -p arcusdb.host=127.0.0.1 \
  -p arcusdb.port=17191 \
  -s
```

YCSB 배포본에 `bin/ycsb.sh`가 없고 `bin/ycsb`만 있다면 원래 YCSB 명령의 앞부분만 `./bin/ycsb`로 바꾸면 됩니다.

## 제공하는 workload

이 저장소는 `scripts/workloads` 아래에 ArcusDB용 workload 파일을 제공합니다. `build.sh`를 실행하면 이 파일들이 YCSB의 `workloads` 디렉터리로 복사됩니다.

기본 제공 파일은 아래와 같습니다.

| 파일 | run 단계 비율 | 설명 |
| --- | --- | --- |
| `arcusdb-workloada` | read 50% / insert 50% | 표준 YCSB A의 update 50%를 현재 지원 가능한 insert 50%로 바꾼 workload입니다. |
| `arcusdb-workloadb` | read 95% / insert 5% | 표준 YCSB B의 update 5%를 현재 지원 가능한 insert 5%로 바꾼 workload입니다. |
| `arcusdb-workloadc` | read 100% | 현재 구현 범위에 가장 잘 맞는 read-only workload입니다. |
| `arcusdb-readonly` | read 100% | 기존 예시 호환용입니다. 새 테스트에서는 `arcusdb-workloadc`를 권장합니다. |

주의할 점은, YCSB 표준 workload A/B는 보통 update를 포함합니다. 하지만 현재 ArcusDB 바인딩에는 `update`가 아직 구현되어 있지 않습니다. 그래서 ArcusDB용 A/B는 write를 `insert`로 잡았습니다.

실제 테스트 크기는 workload 파일을 직접 고치기보다 명령에서 `-p`로 덮어쓰는 것을 권장합니다.

```bash
./bin/ycsb-arcusdb load -P workloads/arcusdb-workloada -p recordcount=1000000
./bin/ycsb-arcusdb run  -P workloads/arcusdb-workloada -p recordcount=1000000 -p operationcount=1000000

./bin/ycsb-arcusdb load -P workloads/arcusdb-workloadb -p recordcount=1000000
./bin/ycsb-arcusdb run  -P workloads/arcusdb-workloadb -p recordcount=1000000 -p operationcount=1000000

./bin/ycsb-arcusdb load -P workloads/arcusdb-workloadc -p recordcount=1000000
./bin/ycsb-arcusdb run  -P workloads/arcusdb-workloadc -p recordcount=1000000 -p operationcount=1000000
```

## 설정 값

이 바인딩에서 받는 설정은 아래 세 가지입니다.

| 설정                      | 기본값         | 설명            |
|-------------------------|-------------|---------------|
| `arcusdb.host`          | `127.0.0.1` | ArcusDB 서버 주소 |
| `arcusdb.port`          | `17191`     | ArcusDB 서버 포트 |
| `arcusdb.timeoutMillis` | `60000`     | 요청 타임아웃       |

YCSB 기본 설정도 함께 사용할 수 있습니다.

| 설정                          | 설명                             |
|-----------------------------|--------------------------------|
| `table`                     | ArcusDB collection 이름으로 사용됩니다. |
| `recordcount`               | load 단계에서 넣을 레코드 수입니다.         |
| `operationcount`            | run 단계에서 실행할 작업 수입니다.          |
| `threadcount` 또는 `-threads` | YCSB 클라이언트 스레드 수입니다.           |

## 데이터 형태

YCSB 레코드는 ArcusDB BSON document로 저장됩니다.

- YCSB key는 document의 `_id` 필드에 저장됩니다.
- YCSB field 값은 문자열로 저장됩니다.
- YCSB `table` 값은 ArcusDB collection 이름으로 사용됩니다.

예를 들어 YCSB가 key `user123`과 field `field0=value0`을 insert하면, ArcusDB에는 개념적으로 아래 document가 들어갑니다.

```json
{
  "_id": "user123",
  "field0": "value0"
}
```

read는 `_id`가 YCSB key와 같은 document를 찾습니다.

## 서버 프로토콜 요약

클라이언트는 ArcusDB 서버에 TCP로 연결합니다.

메시지는 아래 형식으로 전송됩니다.

```text
4-byte big-endian payload length + protobuf payload
```

protobuf 메시지 정의는 `arcusdb/src/main/proto/arcusdb.proto`에 있습니다. document 본문은 protobuf 안에 BSON bytes로
들어갑니다.

## 자주 나는 문제

`Unknown DB`가 나오면 YCSB가 binding 등록이나 jar를 못 찾은 것입니다. `./build.sh`를 다시 실행하고, 원래 YCSB 명령을 쓰는 경우 binding 이름이
`arcusdb`인지 확인하세요.

`Connection refused`가 나오면 ArcusDB 서버가 떠 있지 않거나 host/port가 다릅니다. `arcusdb.host`, `arcusdb.port` 값을
확인하세요.

결과에 `NOT_IMPLEMENTED`가 보이면 현재 미지원 작업을 포함한 workload를 실행한 것입니다. 우선 `arcusdb-workloadc`로 load/run을 확인하세요.

여러 스레드로 실행할 때는 YCSB 스레드마다 ArcusDB TCP 연결이 하나씩 만들어집니다. 서버가 동시에 여러 connection을 받을 수 있어야 합니다.
