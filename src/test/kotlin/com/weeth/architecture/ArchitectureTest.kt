package com.weeth.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * `.claude/rules/architecture.md`의 결정론적 게이트.
 * 규칙 문서가 진실 공급원이며, 이 테스트는 그중 구조적으로 검사 가능한 항목을 강제한다.
 *
 * BASELINE 목록은 도입 시점(2026-06)의 기존 위반으로, 리팩토링 백로그
 * (docs/plan/ai-verification-hardening-plan.md) 대상이다.
 * 항목은 제거만 허용되며 새로 추가하지 않는다.
 */
class ArchitectureTest :
    StringSpec({
        val productionScope = Konsist.scopeFromProduction()

        "계층 의존성: domain·application·infrastructure는 상위 계층에 의존하지 않는다" {
            productionScope.assertArchitecture {
                val domain = Layer("domain", "com.weeth.domain..domain..")
                val application = Layer("application", "com.weeth.domain..application..")
                val presentation = Layer("presentation", "com.weeth.domain..presentation..")
                val infrastructure = Layer("infrastructure", "com.weeth.domain..infrastructure..")

                domain.doesNotDependOn(presentation, infrastructure)
                application.doesNotDependOn(presentation)
                infrastructure.doesNotDependOn(presentation)
            }
        }

        "domain은 application에 의존하지 않는다 (baseline 제외)" {
            productionScope
                .files
                .filter { DOMAIN_LAYER_PATH.containsMatchIn(it.path) }
                .filter { it.name !in DOMAIN_TO_APPLICATION_BASELINE }
                .assertFalse { file ->
                    file.imports.any { it.name.matches(APPLICATION_PACKAGE_IMPORT) }
                }
        }

        "application은 infrastructure에 의존하지 않는다 (baseline 제외)" {
            productionScope
                .files
                .filter { APPLICATION_LAYER_PATH.containsMatchIn(it.path) }
                .filter { it.name !in APPLICATION_TO_INFRASTRUCTURE_BASELINE }
                .assertFalse { file ->
                    file.imports.any { it.name.matches(INFRASTRUCTURE_PACKAGE_IMPORT) }
                }
        }

        "infrastructure는 application에 의존하지 않는다 (baseline 제외)" {
            productionScope
                .files
                .filter { INFRASTRUCTURE_LAYER_PATH.containsMatchIn(it.path) }
                .filter { it.name !in INFRASTRUCTURE_TO_APPLICATION_BASELINE }
                .assertFalse { file ->
                    file.imports.any { it.name.matches(APPLICATION_PACKAGE_IMPORT) }
                }
        }

        "@Transactional은 usecase 패키지에만 붙는다" {
            productionScope
                .classes()
                .filter { cls -> cls.annotations.any { it.name == "Transactional" } }
                .assertTrue { it.resideInPackage("..application.usecase..") }

            productionScope
                .functions()
                .filter { fn -> fn.annotations.any { it.name == "Transactional" } }
                .assertTrue { it.resideInPackage("..application.usecase..") }
        }

        "usecase/command 클래스는 *UseCase 네이밍을 따른다 (baseline 제외)" {
            productionScope
                .classes(includeNested = false)
                .filter { it.resideInPackage("..usecase.command..") }
                .filter { it.name !in COMMAND_NAMING_BASELINE }
                .assertTrue { it.hasNameEndingWith("UseCase") }
        }

        "usecase/query 클래스는 Get*QueryService 네이밍을 따른다" {
            productionScope
                .classes(includeNested = false)
                .filter { it.resideInPackage("..usecase.query..") }
                .assertTrue { it.hasNameStartingWith("Get") && it.hasNameEndingWith("QueryService") }
        }

        "Entity는 data class를 사용하지 않는다" {
            productionScope
                .classes()
                .filter { it.resideInPackage("..domain.entity..") }
                .assertFalse { it.hasDataModifier }
        }

        "domain/port에는 인터페이스만 둔다 (baseline 제외)" {
            productionScope
                .classes()
                .filter { it.name !in PORT_CLASS_BASELINE }
                .assertFalse { it.resideInPackage("..domain.port..") }
        }

        "Port 구현체(*Adapter)는 infrastructure에 둔다" {
            productionScope
                .classes(includeNested = false)
                .filter { it.hasNameEndingWith("Adapter") }
                .assertTrue { it.resideInPackage("..infrastructure..") }
        }

        "Lombok과 MapStruct는 사용하지 않는다" {
            productionScope
                .imports
                .assertFalse { it.name.startsWith("lombok.") || it.name.startsWith("org.mapstruct.") }
        }

        "테스트에서 Mockito를 사용하지 않는다 (MockK만 허용)" {
            Konsist
                .scopeFromTest()
                .imports
                .assertFalse { it.name.startsWith("org.mockito.") }
        }
    }) {
    companion object {
        private val DOMAIN_LAYER_PATH = Regex("com/weeth/domain/[^/]+/domain/")
        private val APPLICATION_LAYER_PATH = Regex("com/weeth/domain/[^/]+/application/")
        private val INFRASTRUCTURE_LAYER_PATH = Regex("com/weeth/domain/[^/]+/infrastructure/")

        private val APPLICATION_PACKAGE_IMPORT = Regex("""com\.weeth\.domain\.[a-z]+\.application\..*""")
        private val INFRASTRUCTURE_PACKAGE_IMPORT = Regex("""com\.weeth\.domain\.[a-z]+\.infrastructure\..*""")

        // 도메인이 던지는 예외가 application/exception에 위치하는 구조적 부채.
        // 해소 방향: 도메인에서 던지는 예외를 domain 계층으로 이동
        private val DOMAIN_TO_APPLICATION_BASELINE =
            setOf(
                "PostRepository",
                "CardinalRepository",
                "ClubRepository",
                "ClubCodePolicy",
                "ClubJoinPolicy",
                "ClubMemberCardinalPolicy",
                "ClubMemberPolicy",
                "ClubPermissionPolicy",
                "FileContentType",
                "FileExtension",
                "SessionRepository",
                "StatusPriority",
                "UserRepository",
            )

        // SocialAuthPortRegistry가 infrastructure에 위치 — Port 추출 대상
        private val APPLICATION_TO_INFRASTRUCTURE_BASELINE =
            setOf(
                "SocialLoginUseCase",
            )

        // 어댑터·스케줄러·리스너가 application(UseCase/DTO/예외)을 직접 참조하는 부채
        private val INFRASTRUCTURE_TO_APPLICATION_BASELINE =
            setOf(
                "AttendanceScheduler",
                "QrExpiredEventListener",
                "S3FileUploadUrlAdapter",
                "CareerNetAdapter",
                "KakaoSocialAuthAdapter",
            )

        // 소문자 Usecase 접미사 — 리네임 대상
        private val COMMAND_NAMING_BASELINE =
            setOf(
                "ManageClubMemberUsecase",
                "GenerateFileUrlUsecase",
            )

        // Port 반환값 VO가 port 패키지에 위치
        private val PORT_CLASS_BASELINE =
            setOf(
                "FileUploadUrl",
            )
    }
}
