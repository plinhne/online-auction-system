package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.server.dao.ItemDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private ItemDAO itemDAO;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemDAO = mock(ItemDAO.class);
        itemService = new ItemService(itemDAO);
    }

    // =================================================
    // 1. GET ITEM BY ID - THÀNH CÔNG
    // =================================================

    @Test
    void test_get_item_by_id_success() throws Exception {

        Item item = mock(Item.class);

        when(itemDAO.findById(1)).thenReturn(item);

        Item result = itemService.getItemById(1);

        assertEquals(item, result);
    }

    // =================================================
    // 2. GET ITEM BY ID - KHÔNG TỒN TẠI
    // =================================================

    @Test
    void test_get_item_by_id_not_found() throws Exception {

        when(itemDAO.findById(1)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> itemService.getItemById(1));
    }

    // =================================================
    // 3. GET BY SELLER
    // =================================================

    @Test
    void test_get_items_by_seller() throws Exception {

        List<Item> items = List.of(mock(Item.class));

        when(itemDAO.findBySellerId(10)).thenReturn(items);

        List<Item> result = itemService.getItemsBySeller(10);

        assertEquals(items, result);
    }

    // =================================================
    // 4. GET BY CATEGORY
    // =================================================

    @Test
    void test_get_items_by_category() throws Exception {

        List<Item> items = List.of(mock(Item.class));

        when(itemDAO.findByCategory(ItemCategory.ELECTRONICS))
                .thenReturn(items);

        List<Item> result = itemService.getItemsByCategory(ItemCategory.ELECTRONICS);

        assertEquals(items, result);
    }

    // =================================================
    // 5. CREATE ITEM - THÀNH CÔNG (SELLER)
    // =================================================

    @Test
    void test_create_item_success() throws Exception {

        User seller = mock(User.class);
        Item item = mock(Item.class);

        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(seller.getId()).thenReturn(1);

        when(itemDAO.save(any(), eq(1), any()))
                .thenReturn(100);

        Item result = itemService.createItem(
                seller,
                "Laptop",
                "Good laptop",
                1000,
                ItemCategory.ELECTRONICS
        );

        assertNotNull(result);

        verify(itemDAO).save(any(), eq(1), eq(ItemCategory.ELECTRONICS));
    }

    // =================================================
    // 6. CREATE ITEM - KHÔNG PHẢI SELLER (BIDDER FAIL)
    // =================================================

    @Test
    void test_create_item_invalid_role_bidder() {

        User user = mock(User.class);

        when(user.getRole()).thenReturn(UserRole.BIDDER);

        assertThrows(IllegalStateException.class,
                () -> itemService.createItem(
                        user,
                        "Name",
                        "Desc",
                        100,
                        ItemCategory.ELECTRONICS
                ));
    }

    // =================================================
    // 7. CREATE ITEM - NAME RỖNG
    // =================================================

    @Test
    void test_create_item_empty_name() {

        User seller = mock(User.class);

        when(seller.getRole()).thenReturn(UserRole.SELLER);

        assertThrows(IllegalArgumentException.class,
                () -> itemService.createItem(
                        seller,
                        " ",
                        "desc",
                        100,
                        ItemCategory.ELECTRONICS
                ));
    }

    // =================================================
    // 8. CREATE ITEM - GIÁ KHÔNG HỢP LỆ
    // =================================================

    @Test
    void test_create_item_invalid_price() {

        User seller = mock(User.class);

        when(seller.getRole()).thenReturn(UserRole.SELLER);

        assertThrows(IllegalArgumentException.class,
                () -> itemService.createItem(
                        seller,
                        "Name",
                        "desc",
                        0,
                        ItemCategory.ELECTRONICS
                ));
    }

    // =================================================
    // 9. UPDATE ITEM - SELLER OK
    // =================================================

    @Test
    void test_update_item_by_seller() throws Exception {

        User seller = mock(User.class);
        Item item = mock(Item.class);

        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(itemDAO.findById(1)).thenReturn(item);

        itemService.updateItem(
                seller,
                1,
                "New Name",
                "New Desc",
                ItemCategory.ELECTRONICS
        );

        verify(item).setName("New Name");
        verify(item).setDescription("New Desc");
        verify(itemDAO).update(item, ItemCategory.ELECTRONICS);
    }

    // =================================================
    // 10. UPDATE ITEM - ADMIN OK
    // =================================================

    @Test
    void test_update_item_by_admin() throws Exception {

        User admin = mock(User.class);
        Item item = mock(Item.class);

        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(itemDAO.findById(1)).thenReturn(item);

        itemService.updateItem(
                admin,
                1,
                "A",
                "B",
                ItemCategory.ELECTRONICS
        );

        verify(itemDAO).update(any(), any());
    }

    // =================================================
    // 11. UPDATE ITEM - BIDDER FAIL
    // =================================================

    @Test
    void test_update_item_by_bidder_fail() throws Exception {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(bidder.getRole()).thenReturn(UserRole.BIDDER);
        when(itemDAO.findById(1)).thenReturn(item);

        assertThrows(IllegalStateException.class,
                () -> itemService.updateItem(
                        bidder,
                        1,
                        "a",
                        "b",
                        ItemCategory.ELECTRONICS
                ));
    }

    // =================================================
    // 12. DELETE ITEM - SELLER OK
    // =================================================

    @Test
    void test_delete_item_by_seller() throws Exception {

        User seller = mock(User.class);
        Item item = mock(Item.class);

        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(itemDAO.findById(1)).thenReturn(item);

        itemService.deleteItem(seller, 1);

        verify(itemDAO).delete(1);
    }

    // =================================================
    // 13. DELETE ITEM - ADMIN OK
    // =================================================

    @Test
    void test_delete_item_by_admin() throws Exception {

        User admin = mock(User.class);
        Item item = mock(Item.class);

        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(itemDAO.findById(1)).thenReturn(item);

        itemService.deleteItem(admin, 1);

        verify(itemDAO).delete(1);
    }

    // =================================================
    // 14. DELETE ITEM - BIDDER FAIL
    // =================================================

    @Test
    void test_delete_item_by_bidder_fail() throws Exception {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(bidder.getRole()).thenReturn(UserRole.BIDDER);
        when(itemDAO.findById(1)).thenReturn(item);

        assertThrows(IllegalStateException.class,
                () -> itemService.deleteItem(bidder, 1));
    }

    // =================================================
    // 15. UPDATE IMAGE URL
    // =================================================

    @Test
    void test_update_image_url() throws Exception {

        User user = mock(User.class);

        itemService.updateImageUrl(user, 1, "image.png");

        verify(itemDAO).updateImageUrl(1, "image.png");
    }
}