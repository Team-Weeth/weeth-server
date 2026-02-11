# Kotlin Test Examples

## DescribeSpec (Recommended for Services)

```kotlin
class UserGetServiceTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val service = UserGetService(userRepository)

    describe("findById") {
        context("when user exists") {
            it("should return user") {
                val user = UserTestFixture.createUser()
                every { userRepository.findByIdAndDeletedAtIsNull(1L) } returns user

                val result = service.findById(1L)

                result shouldBe user
                verify { userRepository.findByIdAndDeletedAtIsNull(1L) }
            }
        }

        context("when user does not exist") {
            it("should throw UserNotFoundException") {
                every { userRepository.findByIdAndDeletedAtIsNull(999L) } returns null

                shouldThrow<UserNotFoundException> {
                    service.findById(999L)
                }
            }
        }
    }
})
```

## BehaviorSpec (BDD style for complex logic)

```kotlin
class CreateUserUsecaseTest : BehaviorSpec({
    val userSaveService = mockk<UserSaveService>()
    val userMapper = mockk<UserMapper>()
    val usecase = CreateUserUsecase(userSaveService, userMapper)

    Given("a valid create user request") {
        val request = CreateUserRequest(name = "John", email = "john@example.com")
        val user = UserTestFixture.createUser()
        val savedUser = user.copy(id = 1L)

        every { userMapper.toEntity(request) } returns user
        every { userSaveService.save(any()) } returns savedUser

        When("creating user") {
            val result = usecase.execute(request)

            Then("user should be created with ID") {
                result.id shouldBe 1L
            }

            Then("save service should be called") {
                verify { userSaveService.save(any()) }
            }
        }
    }

    Given("a request with duplicate email") {
        val request = CreateUserRequest(name = "John", email = "existing@example.com")

        every { userMapper.toEntity(request) } returns UserTestFixture.createUser()
        every { userSaveService.save(any()) } throws UserAlreadyExistsException()

        When("creating user") {
            Then("should throw UserAlreadyExistsException") {
                shouldThrow<UserAlreadyExistsException> {
                    usecase.execute(request)
                }
            }
        }
    }
})
```

## StringSpec (Simple validation tests)

```kotlin
class UserValidationTest : StringSpec({
    "name longer than 100 characters should throw exception" {
        val longName = "a".repeat(101)
        shouldThrow<IllegalArgumentException> {
            User.create(name = longName, email = "test@example.com")
        }
    }

    "empty email should throw exception" {
        shouldThrow<IllegalArgumentException> {
            User.create(name = "John", email = "")
        }
    }

    "valid user should be created successfully" {
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
            context("with valid request") {
                it("should return 200 OK with created user") {
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