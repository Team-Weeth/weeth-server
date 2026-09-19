# V3 → V4 파일 마이그레이터 (WTH-508)

`docs/migration/v3-to-v4/` 의 SQL 스크립트가 끝난 뒤 실행하는 일회성 도구다.

## 왜 SQL로 안 되는가

| | 이유 |
|---|---|
| `file.file_size` | V4에서 `NOT NULL`인데 V3 DB에 값이 없다 → S3 `HeadObject` 필요 |
| `file.content_type` | 동일. 확장자로 추론하되 V4 `FileType` 허용 목록을 통과해야 한다 |
| `storage_key` | V3 평면 키(`{uuid}.{ext}`)와 V4 형식이 달라 **모든 객체를 새 키로 복사**해야 한다 |

```
V3  b2835a47-8728-4570-b584-6c0a39d5dbad.pdf
V4  POST/2025-03/b2835a47-8728-4570-b584-6c0a39d5dbad_영수증.pdf
    {OWNER_TYPE}/{yyyy-MM}/{uuid}_{fileName}
```

`uuid`는 V3 키의 것을 재사용하므로 같은 입력이면 항상 같은 키가 나온다(멱등).
`yyyy-MM`은 V3 `file.created_at` 기준이다.

V3가 원본 파일명을 보존하지 않은 구(舊) 레코드(`file_name`이 곧 S3 키인 경우)는
키에 uuid가 두 번 들어가지 않도록 `{uuid}_file.{ext}` 형태로 만든다.

## 설계

- 운영 아티팩트에 섞이지 않도록 **별도 소스셋**(`src/migration/kotlin`)으로 분리했다. `bootJar`에 포함되지 않는다.
- Spring 컨텍스트를 쓰지 않는다. JDBC + AWS SDK v2만 사용한다.
- **원본 버킷은 읽기만 한다.** 삭제하지 않으므로 롤백은 V4 쪽만 정리하면 된다.
- 동일 계정이면 `CopyObject`(서버사이드)로 객체 바이트가 실행 머신을 거치지 않는다.
  교차 계정에서 프로파일을 둘 지정하면 GetObject → PutObject 스트리밍으로 전환된다.

## AWS 자격증명 (교차 계정)

**V3 버킷과 V4 버킷은 서로 다른 AWS 계정에 있다.** 두 가지 방식을 지원한다.

| | A. 프로파일 2개 (권장) | B. 단일 주체 + 버킷 정책 |
|---|---|---|
| 소스 계정 버킷 정책 수정 | **불필요** | 필요 |
| 전송 경로 | 로컬 경유 | AWS 내부(서버사이드) |
| egress 비용 | 소스 계정에 발생 | 없음 |
| 실행 옵션 | `--source-profile` / `--target-profile` | 옵션 없음 |

406건 규모에서는 A가 단순하다. 남의 계정 버킷 정책을 건드리지 않아도 되고,
각 키가 자기 버킷 권한만 가지면 된다. 도구가 방식을 자동 선택하며 실행 시 어느 쪽인지 출력한다.

### A. 프로파일 2개

```bash
aws configure --profile weeth-v3    # V3 계정 키 — 소스 버킷 읽기
aws configure --profile weeth-v4    # V4 계정 키 — 대상 버킷 쓰기
```

`~/.aws/credentials`

```ini
[weeth-v3]
aws_access_key_id = ...
aws_secret_access_key = ...

[weeth-v4]
aws_access_key_id = ...
aws_secret_access_key = ...
```

필요 권한 — 각 계정에서 **자기 버킷만**:

```
weeth-v3 : s3:ListBucket, s3:GetObject   on weeth-aws-bucket(/*)
weeth-v4 : s3:PutObject                  on <V4버킷>/*
```

실행 시 `--source-profile=weeth-v3 --target-profile=weeth-v4` 를 준다.
두 프로파일이 다르면 자동으로 GetObject → PutObject 스트리밍으로 전환된다.
크기를 미리 알고 있어(HeadObject) 메모리에 전부 적재하지 않는다.

### B. 단일 주체 + 버킷 정책

`CopyObject`는 하나의 주체가 소스 읽기와 대상 쓰기를 모두 수행하므로, 그 주체를 어디에 두느냐가
복사된 객체의 **소유권**을 결정한다. 반드시 **대상(V4) 계정의 IAM 사용자**로 실행한다.
소스(V3) 계정 주체로 실행하면 V4 버킷에 들어간 객체를 V3 계정이 소유하게 되어,
이후 V3 계정을 정리할 때 접근 불능이 된다.

#### 1. V4 계정에 IAM 사용자 + 정책

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "ReadSourceCrossAccount", "Effect": "Allow",
      "Action": ["s3:ListBucket", "s3:GetObject"],
      "Resource": ["arn:aws:s3:::weeth-aws-bucket", "arn:aws:s3:::weeth-aws-bucket/*"] },
    { "Sid": "WriteTarget", "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": "arn:aws:s3:::<V4버킷>/*" }
  ]
}
```

#### 2. V3 계정의 **버킷 정책**에 접근 허용

IAM 정책만으로는 타 계정 버킷에 접근할 수 없다. 양쪽이 모두 허용해야 한다.

```json
{ "Sid": "AllowV4MigrationRead", "Effect": "Allow",
  "Principal": { "AWS": "arn:aws:iam::<V4계정ID>:user/weeth-migration" },
  "Action": ["s3:ListBucket", "s3:GetObject"],
  "Resource": ["arn:aws:s3:::weeth-aws-bucket", "arn:aws:s3:::weeth-aws-bucket/*"] }
```

`ListBucket`의 Resource는 버킷 ARN(`/*` 없음), `GetObject`는 `/*` 포함이다.

#### ⚠️ `s3:ListBucket`은 선택이 아니다 (방식 B)

`s3:GetObject`만 부여하면 **존재하지 않는 객체에 404가 아니라 403이 반환된다**
(S3가 객체 존재 여부를 숨기기 위해). 그러면 `MISSING_IN_S3`로 분류해야 할 건이
전부 `FAILED (HeadObject 실패: 403)`이 되어 **S3 유실 건수를 측정할 수 없다.**

### 공통 주의사항

- 소스 버킷이 고객 관리 KMS 키로 암호화돼 있으면 **V3 계정의 KMS 키 정책**에도 V4 사용자의 `kms:Decrypt`가 필요하다.
- 두 버킷이 같은 리전(`ap-northeast-2`)이면 전송 비용이 없다.
- **검증이 끝날 때까지 소스 버킷을 삭제하지 않는다.** 롤백 창구다.
- 마이그레이션 완료 후 해당 액세스 키를 폐기한다.

## 실행

자격증명은 인자로 받지 않는다. DB 비밀번호는 환경변수, S3는 프로파일 또는 기본 자격증명 체인을 쓴다.

```bash
# 1) 오프라인 — S3 접근 없이 키 생성·소유 매핑만 검토 (자격증명 불필요)
MIG_DB_PASSWORD=... ./gradlew migrateFiles --args="--offline"

# 2) dry-run — HeadObject로 생존·크기를 확인하되 복사·DB 쓰기는 하지 않음
MIG_DB_PASSWORD=... ./gradlew migrateFiles \
  --args="--source-bucket=weeth-aws-bucket --target-bucket=<V4버킷> \
          --source-profile=weeth-v3 --target-profile=weeth-v4"

# 3) 실제 반영
MIG_DB_PASSWORD=... ./gradlew migrateFiles \
  --args="--source-bucket=weeth-aws-bucket --target-bucket=<V4버킷> \
          --source-profile=weeth-v3 --target-profile=weeth-v4 --apply"
```

방식 B(단일 주체 + 버킷 정책)를 쓸 때는 프로파일 옵션을 빼고 `AWS_PROFILE`만 지정한다.

```bash
AWS_PROFILE=weeth-migration MIG_DB_PASSWORD=... ./gradlew migrateFiles \
  --args="--source-bucket=weeth-aws-bucket --target-bucket=<V4버킷>"
```

**기본값이 dry-run이다.** 실제 반영은 `--apply`를 명시해야 한다.

### 옵션

| 옵션 | 환경변수 | 기본값 |
|---|---|---|
| `--work-url` | `MIG_WORK_URL` | `jdbc:mysql://localhost:3306/weeth_migration` |
| `--target-url` | `MIG_TARGET_URL` | `jdbc:mysql://localhost:3306/weeth_v4_mig` |
| `--v3-schema` | `MIG_V3_SCHEMA` | `weeth_v3` |
| `--db-user` | `MIG_DB_USER` | `root` |
| (비밀번호) | `MIG_DB_PASSWORD` | 빈 문자열 |
| `--source-bucket` | `MIG_SOURCE_BUCKET` | 필수 |
| `--target-bucket` | `MIG_TARGET_BUCKET` | 필수 |
| `--region` | `AWS_REGION` | `ap-northeast-2` |
| `--source-profile` | `MIG_SOURCE_PROFILE` | 기본 자격증명 체인 |
| `--target-profile` | `MIG_TARGET_PROFILE` | 소스와 동일 |
| `--limit=N` | | 0(전체) |
| `--offline` | | 꺼짐 |
| `--apply` | | 꺼짐(=dry-run) |

두 프로파일이 다르면 교차 계정으로 판단해 스트리밍 복사로 전환한다. 실행 시 어느 방식인지 출력한다.

## 상태 전이

`weeth_migration.mig_file_map.status`

```
PENDING ──HeadObject OK──> COPIED ──INSERT──> INSERTED
   │
   ├─ 객체 없음(404) ─────> MISSING_IN_S3
   ├─ 허용 목록 밖 확장자 ─> UNSUPPORTED_TYPE
   └─ 0바이트 / 복사 실패 ─> FAILED
```

재실행하면 `PENDING`만 다시 처리한다. `CopyObject`는 덮어쓰기라 안전하고,
V4 `file.storage_key`가 `UNIQUE`이므로 중복 INSERT도 차단된다.

## 파일 형식 정책

허용 목록이 **세 곳**에 있으며 항상 일치해야 한다. 하나라도 어긋나면
SQL이 "이전 가능"으로 분류한 파일을 마이그레이터가 거부한다.

| 위치 | 역할 |
|---|---|
| `FileType.kt` | V4 업로드 검증 (production) |
| `ContentTypes.kt` | 마이그레이터 |
| `10_file_prepare.sql` | 사전 분류 |

현재 허용: `jpg` `jpeg` `png` `webp` `pdf` `pptx` `docx`

실측(406건) 기준 이전 불가 2건:

| 확장자 | 건수 | 제외 사유 |
|---|---:|---|
| svg | 1 | XML이라 `<script>` 삽입이 가능해 인라인 서빙 시 XSS 벡터 |
| mp4 | 1 | 용량·전송비·Range 요청. 허용하면 이후 업로드도 열린다 |

이 외에 비표준 키(한글 파일명이 URL 인코딩된 구 레코드) 5건은 `FAILED`로 남기고 이전하지 않는다.
