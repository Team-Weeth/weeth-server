package com.weeth.tools.filemigration

/**
 * 파일 마이그레이터 실행 설정.
 *
 * 자격증명은 인자로 받지 않는다. DB는 `~/.my.cnf` 대신 JDBC URL/계정을 환경변수로,
 * S3는 AWS SDK 기본 자격증명 체인(환경변수 · 프로파일 · IAM 역할)을 사용한다.
 */
data class MigrationConfig(
    /** 작업 스키마(mig_file_map 보유) JDBC URL */
    val workJdbcUrl: String,
    /** V4 대상 스키마 JDBC URL */
    val targetJdbcUrl: String,
    /** V3 원본 스키마명. 작업 스키마와 같은 서버에 있어야 한다(JOIN에 사용). */
    val v3Schema: String,
    val dbUser: String,
    val dbPassword: String,
    /** V3 원본 버킷 */
    val sourceBucket: String,
    /** V4 대상 버킷. 원본과 같아도 키가 달라 복사는 필요하다. */
    val targetBucket: String,
    val region: String,
    /**
     * 소스/대상 버킷이 서로 다른 AWS 계정에 있을 때 각각의 프로파일을 지정한다.
     * 둘이 다르면 `CopyObject`(서버사이드) 대신 GetObject → PutObject 스트리밍으로 전환된다.
     * 소스 계정 버킷 정책을 수정할 수 없을 때 쓰는 경로다.
     * 지정하지 않으면 기본 자격증명 체인 하나를 양쪽에 사용한다.
     */
    val sourceProfile: String?,
    val targetProfile: String?,
    /** true면 S3 복사와 DB 쓰기를 수행하지 않고 계획만 출력한다. */
    val dryRun: Boolean,
    /**
     * true면 S3에 전혀 접근하지 않는다(HeadObject 포함).
     * 자격증명 없이 키 생성·소유 대상 매핑만 검토할 때 사용한다. 항상 dry-run이다.
     */
    val offline: Boolean,
    /** 0이면 전체 처리 */
    val limit: Int,
) {
    companion object {
        private const val DEFAULT_REGION = "ap-northeast-2"

        fun fromArgs(args: Array<String>): MigrationConfig {
            val opts =
                args
                    .filter { it.startsWith("--") }
                    .associate { arg ->
                        val idx = arg.indexOf('=')
                        if (idx <
                            0
                        ) {
                            arg.removePrefix("--") to "true"
                        } else {
                            arg.substring(2, idx) to arg.substring(idx + 1)
                        }
                    }

            fun opt(
                name: String,
                env: String,
                default: String? = null,
            ): String =
                opts[name]
                    ?: System.getenv(env)
                    ?: default
                    ?: error("필수 설정 누락: --$name 또는 환경변수 $env")

            // 오프라인 모드에서는 S3에 접근하지 않으므로 버킷명이 필요 없다.
            val offline = opts.containsKey("offline")
            val bucketDefault = if (offline) "(offline)" else null

            return MigrationConfig(
                workJdbcUrl = opt("work-url", "MIG_WORK_URL", "jdbc:mysql://localhost:3306/weeth_migration"),
                targetJdbcUrl = opt("target-url", "MIG_TARGET_URL", "jdbc:mysql://localhost:3306/weeth_v4_mig"),
                v3Schema = opt("v3-schema", "MIG_V3_SCHEMA", "weeth_v3"),
                dbUser = opt("db-user", "MIG_DB_USER", "root"),
                dbPassword = opts["db-password"] ?: System.getenv("MIG_DB_PASSWORD") ?: "",
                sourceBucket = opt("source-bucket", "MIG_SOURCE_BUCKET", bucketDefault),
                targetBucket = opt("target-bucket", "MIG_TARGET_BUCKET", bucketDefault),
                region = opt("region", "AWS_REGION", DEFAULT_REGION),
                sourceProfile = opts["source-profile"] ?: System.getenv("MIG_SOURCE_PROFILE"),
                targetProfile = opts["target-profile"] ?: System.getenv("MIG_TARGET_PROFILE"),
                // 기본값이 dry-run이다. 실제 반영은 --apply 를 명시해야 한다.
                dryRun = offline || !opts.containsKey("apply"),
                offline = offline,
                limit = opts["limit"]?.toIntOrNull() ?: 0,
            )
        }
    }
}
