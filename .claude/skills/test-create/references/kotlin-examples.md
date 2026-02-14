# Kotlin Test Examples

## DescribeSpec (Recommended for UseCases)

```kotlin
class CreateUserUseCaseTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val userMapper = mockk<UserMapper>()
    val useCase = CreateUserUseCase(userRepository, userMapper)

    describe("execute 실행") {
        context("유효한 요청이 주어졌을 때") {
            it("사용자를 생성하고 저장한다") {
                val request = UserTestFixture.createRequest()
                val user = UserTestFixture.createUser()
                every { userRepository.save(any()) } returns user
                every { userMapper.toResponse(any()) } returns UserResponse(id = 1L, name = "Test User")

                val result = useCase.execute(request)

                result.id shouldBe 1L
                verify { userRepository.save(any()) }
            }
        }

        context("검증에 실패했을 때") {
            it("IllegalArgumentException을 던진다") {
                val request = UserTestFixture.createRequest(name = "")

                shouldThrow<IllegalArgumentException> {
                    useCase.execute(request)
                }
            }
        }
    }
})
```

## BehaviorSpec (BDD style for complex logic)

```kotlin
class CreateUserUseCaseBddTest : BehaviorSpec({
    val userRepository = mockk<UserRepository>()
    val userMapper = mockk<UserMapper>()
    val useCase = CreateUserUseCase(userRepository, userMapper)

    Given("유효한 사용자 생성 요청이 주어졌을 때") {
        val request = CreateUserRequest(name = "John", email = "john@example.com")
        val user = UserTestFixture.createUser()

        every { userRepository.save(any()) } returns user
        every { userMapper.toResponse(any()) } returns UserResponse(id = 1L, name = "John")

        When("사용자를 생성하면") {
            val result = useCase.execute(request)

            Then("ID가 포함된 사용자 응답이 반환되어야 한다") {
                result.id shouldBe 1L
            }

            Then("repository의 save가 호출되어야 한다") {
                verify { userRepository.save(any()) }
            }
        }
    }

    Given("중복된 이메일 요청이 주어졌을 때") {
        val request = CreateUserRequest(name = "John", email = "existing@example.com")

        every { userRepository.save(any()) } throws DataIntegrityViolationException("duplicate")

        When("사용자를 생성하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<DataIntegrityViolationException> {
                    useCase.execute(request)
                }
            }
        }
    }
})
```

## StringSpec (Simple validation tests)

```kotlin
class UserValidationTest : StringSpec({
    "이름이 100자를 초과하면 예외가 발생한다" {
        val longName = "a".repeat(101)
        shouldThrow<IllegalArgumentException> {
            User.create(name = longName, email = "test@example.com")
        }
    }

    "이메일이 비어 있으면 예외가 발생한다" {
        shouldThrow<IllegalArgumentException> {
            User.create(name = "John", email = "")
        }
    }

    "유효한 사용자면 정상 생성된다" {
        val user = User.create(name = "John", email = "john@example.com")
        user.name shouldBe "John"
        user.email shouldBe "john@example.com"
    }
})
```

## Controller Test

```kotlin
@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest : DescribeSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var createUserUsecase: CreateUserUsecase

    init {
        describe("POST /api/v1/users") {
            context("유효한 요청이 주어졌을 때") {
                it("생성된 사용자와 함께 200 OK를 반환한다") {
                    val request = CreateUserRequest(name = "John", email = "john@example.com")
                    val response = UserResponse(id = 1L, name = "John")
                    every { createUserUsecase.execute(any()) } returns response

                    mockMvc.perform(
                        post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(1))
                        .andExpect(jsonPath("$.name").value("John"))
                }
            }
        }
    }
}
```

## Test Fixture

```kotlin
object UserTestFixture {
    fun createUser(
        id: Long = 1L,
        email: String = "test@example.com",
        name: String = "Test User",
        status: UserStatus = UserStatus.ACTIVE
    ) = User(
        id = id,
        name = name,
        email = email,
        status = status
    )

    fun createRequest(
        name: String = "Test User",
        email: String = "test@example.com"
    ) = CreateUserRequest(name = name, email = email)

    fun createUsers(count: Int = 3) =
        (1..count).map { createUser(id = it.toLong(), email = "user$it@example.com") }
}
```

## MockK Usage

```kotlin
// Create mock
val repository = mockk<UserRepository>()

// Relaxed mock (returns default values for all methods)
val relaxedMock = mockk<SomeService>(relaxed = true)

// Stubbing
every { repository.findById(1L) } returns Optional.of(user)
every { repository.save(any()) } returns user
every { repository.findById(any()) } returns Optional.empty()

// Stubbing with argument capture
val slot = slot<User>()
every { repository.save(capture(slot)) } answers { slot.captured }

// Verify
verify { repository.save(any()) }
verify(exactly = 1) { repository.findById(1L) }
verify(exactly = 0) { repository.delete(any()) }

// Verify order
verifyOrder {
    repository.findById(1L)
    repository.save(any())
}

// Clear mocks
clearMocks(repository)
```
