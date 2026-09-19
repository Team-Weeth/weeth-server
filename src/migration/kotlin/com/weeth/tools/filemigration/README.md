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
- `CopyObject`는 서버사이드다. 객체 바이트가 실행 머신을 거치지 않는다.

## AWS 자격증명 (교차 계정)

**V3 버킷과 V4 버킷은 서로 다른 AWS 계정에 있다.**

`CopyObject`는 하나의 주체가 소스 읽기와 대상 쓰기를 모두 수행하므로, 그 주체를 어디에 두느냐가
복사된 객체의 **소유권**을 결정한다. 반드시 **대상(V4) 계정의 IAM 사용자**로 실행한다.
소스(V3) 계정 주체로 실행하면 V4 버킷에 들어간 객체를 V3 계정이 소유하게 되어,
이후 V3 계정을 정리할 때 접근 불능이 된다.

### 1. V4 계정에 IAM 사용자 + 정책

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

### 2. V3 계정의 **버킷 정책**에 접근 허용

IAM 정책만으로는 타 계정 버킷에 접근할 수 없다. 양쪽이 모두 허용해야 한다.

```json
{ "Sid": "AllowV4MigrationRead", "Effect": "Allow",
  "Principal": { "AWS": "arn:aws:iam::<V4계정ID>:user/weeth-migration" },
  "Action": ["s3:ListBucket", "s3:GetObject"],
  "Resource": ["arn:aws:s3:::weeth-aws-bucket", "arn:aws:s3:::weeth-aws-bucket/*"] }
```

`ListBucket`의 Resource는 버킷 ARN(`/*` 없음), `GetObject`는 `/*` 포함이다.

### ⚠️ `s3:ListBucket`은 선택이 아니다

`s3:GetObject`만 부여하면 **존재하지 않는 객체에 404가 아니라 403이 반환된다**
(S3가 객체 존재 여부를 숨기기 위해). 그러면 `MISSING_IN_S3`로 분류해야 할 건이
전부 `FAILED (HeadObject 실패: 403)`이 되어 **S3 유실 건수를 측정할 수 없다.**

### 기타

- 소스 버킷이 고객 관리 KMS 키로 암호화돼 있으면 **V3 계정의 KMS 키 정책**에도 V4 사용자의 `kms:Decrypt`가 필요하다.
- 두 버킷이 같은 리전(`ap-northeast-2`)이면 전송 비용이 없다.
- **검증이 끝날 때까지 소스 버킷을 삭제하지 않는다.** 롤백 창구다.
- 마이그레이션 완료 후 해당 액세스 키를 폐기한다.

## 실행

자격증명은 인자로 받지 않는다. DB는 환경변수, S3는 AWS SDK 기본 자격증명 체인을 쓴다.

```bash
aws configure --profile weeth-migration   # V4 계정 사용자 키
export AWS_PROFILE=weeth-migration
```

```bash
# 1) 오프라인 — S3 접근 없이 키 생성·소유 매핑만 검토 (자격증명 불필요)
MIG_DB_PASSWORD=... ./gradlew migrateFiles --args="--offline"

# 2) dry-run — HeadObject로 생존·크기를 확인하되 복사·DB쓰기는 하지 않음
MIG_DB_PASSWORD=... ./gradlew migrateFiles \
  --args="--source-bucket=weeth-aws-bucket --target-bucket=<V4버킷>"

# 3) 실제 반영
MIG_DB_PASSWORD=... ./gradlew migrateFiles \
  --args="--source-bucket=weeth-aws-bucket --target-bucket=<V4버킷> --apply"
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
| `--limit=N` | | 0(전체) |
| `--offline` | | 꺼짐 |
| `--apply` | | 꺼짐(=dry-run) |

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

`ContentTypes.SUPPORTED`가 V4 `FileType`과 일치해야 한다. 현재는 jpg/jpeg/png/webp/pdf 5종이다.

실측(406건) 기준 이전 불가 31건:

| 확장자 | 건수 | 판단 |
|---|---:|---|
| pptx | 27 | `FileType` 추가 권장 (세션 자료로 추정) |
| docx | 2 | 추가에 문제 없음 |
| mp4 | 1 | 별도 판단(용량·전송비·Range 요청) |
| svg | 1 | **추가 비권장** — XML이라 `<script>` 삽입이 가능해 인라인 서빙 시 XSS 벡터 |

`FileType`을 확장하기로 하면 `ContentTypes`와 `10_file_prepare.sql` 의 허용 목록을 함께 수정한다.
