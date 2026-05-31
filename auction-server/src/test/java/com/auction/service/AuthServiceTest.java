package com.auction.service;

import com.auction.exception.UnauthorizedException;
import com.auction.model.user.User;
import com.auction.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserService userService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        authService = new AuthService(userService);
    }

    // =================================================
    // 1. USER KHÔNG TỒN TẠI
    // =================================================

    @Test
    void test_user_khong_ton_tai() throws SQLException {

        when(userService.findByEmail("a@gmail.com"))
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("a@gmail.com", "123")
        );

        assertEquals("Account does not exist!", ex.getMessage());
    }

    // =================================================
    // 2. SAI MẬT KHẨU
    // =================================================

    @Test
    void test_sai_mat_khau() throws SQLException {

        User user = mock(User.class);

        when(userService.findByEmail("a@gmail.com"))
                .thenReturn(user);

        when(user.getPassword()).thenReturn("correct");

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("a@gmail.com", "wrong")
        );

        assertEquals("Incorrect password!", ex.getMessage());
    }

    // =================================================
    // 3. USER BỊ LOCK / KHÔNG ACTIVE
    // =================================================

    @Test
    void test_tai_khoan_bi_khoa() throws SQLException {

        User user = mock(User.class);

        when(userService.findByEmail("a@gmail.com"))
                .thenReturn(user);

        when(user.getPassword()).thenReturn("123");
        when(user.getStatus()).thenReturn(UserStatus.BANNED);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("a@gmail.com", "123")
        );

        assertEquals("Account is locked by Admin!", ex.getMessage());
    }

    // =================================================
    // 4. LOGIN THÀNH CÔNG
    // =================================================

    @Test
    void test_login_thanh_cong() throws SQLException {

        User user = mock(User.class);

        when(userService.findByEmail("a@gmail.com"))
                .thenReturn(user);

        when(user.getPassword()).thenReturn("123");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getName()).thenReturn("Nguyen");

        User result = authService.login("a@gmail.com", "123");

        assertEquals(user, result);

        verify(userService).findByEmail("a@gmail.com");
    }

    // =================================================
    // 5. VERIFY FULL FLOW
    // =================================================

    @Test
    void test_verify_day_du_quy_trinh() throws SQLException {

        User user = mock(User.class);

        when(userService.findByEmail(anyString()))
                .thenReturn(user);

        when(user.getPassword()).thenReturn("pass");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getName()).thenReturn("TestUser");

        authService.login("x@gmail.com", "pass");

        verify(userService).findByEmail("x@gmail.com");
        verify(user).getPassword();
        verify(user).getStatus();
        verify(user).getName();
    }

    // =================================================
    // 6. EDGE CASE: PASSWORD NULL (an toàn hệ thống)
    // =================================================

    @Test
    void test_password_null() throws SQLException {

        User user = mock(User.class);

        when(userService.findByEmail(anyString()))
                .thenReturn(user);

        when(user.getPassword()).thenReturn(null);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        assertThrows(
                NullPointerException.class,
                () -> authService.login("a@gmail.com", "123")
        );
    }
}