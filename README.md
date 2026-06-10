# ArcusDB YCSB Binding

ArcusDB 서버를 YCSB로 테스트하기 위한 Java 클라이언트 바인딩입니다.

YCSB는 데이터베이스 서버를 직접 호출하지 않습니다. YCSB가 `insert`, `read` 같은 작업을 호출하면, 이 프로젝트의 `ArcusYcsbClient`가 해당 작업을 ArcusDB TCP/protobuf 요청으로 변환해 서버에 보냅니다.

## 지원 범위

현재 구현된 YCSB 작업은 아래와 같습니다.

| YCSB 작업  | 상태  | 비고                                 |
|----------|-----|------------------------------------|
| `insert` | 지원  | YCSB load 단계에서 사용                  |
| `read`   | 지원  | `_id == key` 조건으로 조회               |
| `update` | 미지원 | YCSB 기본 workload A/B에는 update가 포함됨 |
| `scan`   | 미지원 |                                    |
| `delete` | 미지원 |                                    |

YCSB 기본 `workloada`, `workloadb`는 update를 포함하므로 현재 바인딩에는 맞지 않습니다. 대신 이 저장소에서 제공하는 ArcusDB용 workload를 사용하세요.

## 준비물

- JDK 17
- Maven 3.x
- 실행 중인 ArcusDB 서버
- YCSB 0.17.0 배포본

`build.sh`는 `./ycsb-0.17.0`이 없으면 YCSB 0.17.0 배포본을 자동으로 다운로드합니다.

## 설치

```bash
./build.sh
```

이미 받은 YCSB가 다른 위치에 있다면 경로를 넘길 수 있습니다.

```bash
./build.sh /path/to/ycsb-0.17.0
```

`build.sh`가 수행하는 작업은 아래와 같습니다.

1. `arcusdb` 모듈 빌드
2. `arcusdb-ycsb-binding.jar` 생성
3. YCSB에 ArcusDB binding jar 설치
4. YCSB `bin/bindings.properties`에 `arcusdb` 등록
5. `scripts/workloads`의 ArcusDB용 workload 복사
6. `bin/ycsb-arcusdb` 실행 wrapper 생성

기존 YCSB `lib` 전체를 삭제하지 않습니다. ArcusDB 관련 파일은 `arcusdb-binding/lib`에 따로 설치됩니다.

## 실행

상세 테스트 명령은 [test.md](./test.md)를 참고하세요.

기본 흐름은 아래와 같습니다.

```bash
./build.sh
cd ycsb-0.17.0

./bin/ycsb-arcusdb load -P workloads/arcusdb-workloadc -p table=test
./bin/ycsb-arcusdb run  -P workloads/arcusdb-workloadc -p table=test
```

실제 서버 주소, 포트, record 수, operation 수는 실행 시 `-p` 옵션으로 지정합니다.

## 제공 workload

| 파일                  | run 단계 비율             |
|---------------------|-----------------------|
| `arcusdb-workloada` | read 50% / insert 50% |
| `arcusdb-workloadb` | read 95% / insert 5%  |
| `arcusdb-workloadc` | read 100%             |

표준 YCSB workload A/B는 `update`를 사용합니다. 현재 바인딩은 `update`를 지원하지 않으므로, 위 ArcusDB용 workload를 사용해야 합니다.

## 주요 설정

| 설정                      | 설명                       |
|-------------------------|--------------------------|
| `table`                 | ArcusDB collection 이름    |
| `recordcount`           | load 단계에서 넣을 record 수    |
| `operationcount`        | run 단계에서 실행할 operation 수 |
| `arcusdb.host`          | ArcusDB 서버 host          |
| `arcusdb.port`          | ArcusDB 서버 port          |
| `arcusdb.timeoutMillis` | 요청 timeout               |
| `-threads`              | YCSB 클라이언트 thread 수      |

## 데이터 형태

YCSB record는 ArcusDB BSON document로 저장됩니다.

- YCSB key는 `_id` 필드에 저장됩니다.
- YCSB field 값은 문자열로 저장됩니다.
- YCSB `table` 값은 ArcusDB collection 이름으로 사용됩니다.

예:

```json
{
  "_id": "user123",
  "field0": "value0"
}
```