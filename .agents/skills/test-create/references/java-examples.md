# Java Test Examples

## JUnit 5 + Mockito Service Test

```java
@ExtendWith(MockitoExtension.class)
class UserGetServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserGetService userGetService;

    @Test
    @DisplayName("should return user when exists")
    void findById_whenUserExists_shouldReturnUser() {
        // given
        User user = UserTestFixture.createUser(1L, "test@example.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        // when
        User result = userGetService.findById(1L);

        // then
        assertThat(result).isEqualTo(user);
        verify(userRepository).findByIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("should throw exception when user not found")
    void findById_whenUserNotFound_shouldThrowException() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userGetService.findById(999L))
            .isInstanceOf(UserNotFoundException.class);
    }
}
```

## Controller Test

```java
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUserUsecase createUserUsecase;

    @Test
    @DisplayName("POST /api/v1/users - should create user")
    void createUser_shouldReturnCreatedUser() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("John", "john@example.com");
        UserResponse response = UserResponse.builder().id(1L).name("John").build();
        when(createUserUsecase.execute(any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John"));
    }
}
```

## Test Fixture

```java
public class UserTestFixture {
    public static User createUser(Long id, String email) {
        return User.builder()
            .id(id)
            .name("Test User")
            .email(email)
            .status(UserStatus.ACTIVE)
            .build();
    }

    public static User createUser() {
        return createUser(1L, "test@example.com");
    }

    public static CreateUserRequest createRequest() {
        return new CreateUserRequest("Test User", "test@example.com");
    }
}
```

## Mockito Usage

```java
// Stubbing
when(repository.findById(1L)).thenReturn(Optional.of(user));
when(repository.save(any())).thenReturn(user);

// Verify
verify(repository).save(any());
verify(repository, times(1)).findById(1L);
verify(repository, never()).delete(any());

// Argument capture
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repository).save(captor.capture());
User captured = captor.getValue();

// BDD style
given(repository.findById(1L)).willReturn(Optional.of(user));
then(repository).should().save(any());
```