package leets.weeth.domain.user.domain.service;

import leets.weeth.domain.user.application.exception.UserNotFoundException;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserGetServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserGetService userGetService;

	@Test
	@DisplayName("find(Long Id) : 존재하지 않는 유저일 때 예외를 던진다")
	void find_id_userNotFound_throwsException() {
		//given
		Long userId = 1L;
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThrows(UserNotFoundException.class, () -> userGetService.find(userId));
	}

	@Test
	@DisplayName("find(String email) : 존재하지 않는 유저일 때 예외를 던진다")
	void find_email_userNotFound_throwsException() {
		//given
		String email = "test@test.com";
		given(userRepository.findByEmail(email)).willReturn(Optional.empty());

		//when & then
		assertThrows(UserNotFoundException.class, () -> userGetService.find(email));
	}

	@Test
	@DisplayName("findAll(Pageable pageable) : 빈 슬라이스 반환 시 , 유저 예외 던진다")
	void findAll_userNotFound_throwsException() {
		//given
		Pageable pageable = PageRequest.of(0, 10);
		Slice<User> emptySlice = new SliceImpl<>(List.of(), pageable, false);

		given(userRepository.findAllByStatusOrderedByCardinalAndName(any(), eq(pageable)))
			.willReturn(emptySlice);

		//when & then
		assertThrows(UserNotFoundException.class,
			() -> userGetService.findAll(pageable));

	}

}
