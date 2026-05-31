package com.auction.service;

import com.auction.model.user.*;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserDAO userDAO;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        userService = new UserService(userDAO);
    }

    // =================================================
    // 1. REGISTER - EMAIL ĐÃ TỒN TẠI
    // =================================================

    @Test
    void test_register_email_exists() throws Exception {

        when(userDAO.findByEmail("a@gmail.com"))
                .thenReturn(mock(User.class));

        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(
                        "A", "a@gmail.com", "123", UserRole.BIDDER
                ));
    }

    // =================================================
    // 2. REGISTER BIDDER
    // =================================================

    @Test
    void test_register_bidder_success() throws Exception {

        when(userDAO.findByEmail("b@gmail.com"))
                .thenReturn(null);

        User result = userService.registerUser(
                "B",
                "b@gmail.com",
                "123",
                UserRole.BIDDER
        );

        assertTrue(result instanceof Bidder);
        verify(userDAO).save(any(Bidder.class));
    }

    // =================================================
    // 3. REGISTER SELLER
    // =================================================

    @Test
    void test_register_seller_success() throws Exception {

        when(userDAO.findByEmail("s@gmail.com"))
                .thenReturn(null);

        User result = userService.registerUser(
                "S",
                "s@gmail.com",
                "123",
                UserRole.SELLER
        );

        assertTrue(result instanceof Seller);
        verify(userDAO).save(any(Seller.class));
    }

    // =================================================
    // 4. REGISTER ADMIN
    // =================================================

    @Test
    void test_register_admin_success() throws Exception {

        when(userDAO.findByEmail("admin@gmail.com"))
                .thenReturn(null);

        User result = userService.registerUser(
                "Admin",
                "admin@gmail.com",
                "123",
                UserRole.ADMIN
        );

        assertTrue(result instanceof Admin);
        verify(userDAO).save(any(Admin.class));
    }

    // =================================================
    // 5. FIND BY ID
    // =================================================

    @Test
    void test_find_by_id() throws Exception {

        User user = mock(User.class);

        when(userDAO.findById(1)).thenReturn(user);

        assertEquals(user, userService.findById(1));
    }

    // =================================================
    // 6. FIND BY EMAIL
    // =================================================

    @Test
    void test_find_by_email() throws Exception {

        User user = mock(User.class);

        when(userDAO.findByEmail("a@gmail.com"))
                .thenReturn(user);

        assertEquals(user, userService.findByEmail("a@gmail.com"));
    }

    // =================================================
    // 7. FIND ALL
    // =================================================

    @Test
    void test_find_all() throws Exception {

        List<User> users = List.of(mock(User.class));

        when(userDAO.findAll()).thenReturn(users);

        assertEquals(users, userService.findAll());
    }

    // =================================================
    // 8. UPDATE USER
    // =================================================

    @Test
    void test_update_user() throws Exception {

        userService.updateUser(1, "NewName", "SELLER");

        verify(userDAO).update(1, "NewName", "SELLER");
    }

    // =================================================
    // 9. DELETE USER
    // =================================================

    @Test
    void test_delete_user() throws Exception {

        userService.deleteUser(1);

        verify(userDAO).delete(1);
    }

    // =================================================
    // 10. UPDATE BALANCE OK
    // =================================================

    @Test
    void test_update_balance_success() throws Exception {

        userService.updateBalance(1, 1000);

        verify(userDAO).updateBalance(1, 1000);
    }

    // =================================================
    // 11. UPDATE BALANCE FAIL (NEGATIVE)
    // =================================================

    @Test
    void test_update_balance_negative() throws Exception {

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateBalance(1, -100));
    }
}