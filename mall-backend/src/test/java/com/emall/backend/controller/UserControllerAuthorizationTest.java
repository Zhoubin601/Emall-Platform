package com.emall.backend.controller;

import com.emall.backend.entity.User;
import com.emall.backend.mapper.UserMapper;
import com.emall.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerAuthorizationTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserController controller = new UserController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    @Test
    void regularAdministratorCannotPromoteSelf() {
        User existing = user(2L, 1);
        when(userMapper.selectById(2L)).thenReturn(existing);

        User update = user(2L, 2);
        update.setUsername("promoted-admin");
        update.setStatus(0);
        update.setNickname("Allowed nickname change");

        controller.updateInfo(update, authentication(2L, 1));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRole()).isNull();
        assertThat(captor.getValue().getUsername()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getNickname()).isEqualTo("Allowed nickname change");
    }

    @Test
    void regularAdministratorCannotUpdateAnotherUser() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, 0));

        assertThatThrownBy(() -> controller.updateInfo(user(7L, 1), authentication(2L, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void superAdministratorCannotCreateAnotherSuperAdministrator() {
        User requested = user(null, 2);
        requested.setUsername("second-root");
        requested.setPassword("password123");

        assertThatThrownBy(() -> controller.addUser(requested, authentication(1L, 2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void superAdministratorCannotDeleteASuperAdministrator() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, 2));

        assertThatThrownBy(() -> controller.deleteUser(1L, authentication(1L, 2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(userMapper, never()).deleteById(1L);
    }

    @Test
    void regularAdministratorCannotUseUserManagementOperations() {
        var regularAdmin = authentication(2L, 1);
        User requested = user(null, 0);
        requested.setUsername("new-user");
        requested.setPassword("password123");

        assertThatThrownBy(() -> controller.getUserList(regularAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> controller.addUser(requested, regularAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> controller.deleteUser(7L, regularAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void superAdministratorCanListUsers() {
        when(userMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        assertThat(controller.getUserList(authentication(1L, 2))).isEmpty();
    }

    private User user(Long id, int role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private UsernamePasswordAuthenticationToken authentication(Long id, int role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(id, "tester", role), null, List.of());
    }
}
