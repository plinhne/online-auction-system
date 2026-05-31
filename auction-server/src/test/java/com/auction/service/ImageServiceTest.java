package com.auction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceTest {

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService();
    }

    @AfterEach
    void cleanUp() throws Exception {
        // xoá toàn bộ file test sau khi chạy
        File dir = new File("uploads/items/");
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
        }
    }

    // =================================================
    // 1. TEST SAVE IMAGE THÀNH CÔNG
    // =================================================

    @Test
    void test_save_image_success() throws Exception {

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("hello image".getBytes());

        String path = imageService.saveImage(base64, 1, "png");

        // kiểm tra file tồn tại
        assertTrue(Files.exists(Path.of(path)));

        // kiểm tra nội dung file đúng
        byte[] data = Files.readAllBytes(Path.of(path));
        assertEquals("hello image", new String(data));
    }

    // =================================================
    // 2. TEST DELETE IMAGE JPG
    // =================================================

    @Test
    void test_delete_image_jpg() throws Exception {

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("img".getBytes());

        imageService.saveImage(base64, 2, "jpg");

        imageService.deleteImage(2);

        assertFalse(Files.exists(Path.of("uploads/items/2.jpg")));
    }

    // =================================================
    // 3. TEST DELETE IMAGE PNG
    // =================================================

    @Test
    void test_delete_image_png() throws Exception {

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("img".getBytes());

        imageService.saveImage(base64, 3, "png");

        imageService.deleteImage(3);

        assertFalse(Files.exists(Path.of("uploads/items/3.png")));
    }

    // =================================================
    // 4. DELETE KHI FILE KHÔNG TỒN TẠI
    // =================================================

    @Test
    void test_delete_image_not_exist() {

        // không crash
        assertDoesNotThrow(() -> imageService.deleteImage(999));
    }

    // =================================================
    // 5. TEST SAVE IMAGE NHIỀU LẦN
    // =================================================

    @Test
    void test_save_multiple_images() throws Exception {

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("multi".getBytes());

        String p1 = imageService.saveImage(base64, 10, "jpg");
        String p2 = imageService.saveImage(base64, 11, "png");

        assertTrue(Files.exists(Path.of(p1)));
        assertTrue(Files.exists(Path.of(p2)));
    }

    // =================================================
    // 6. TEST BASE64 RỖNG (EDGE CASE)
    // =================================================

    @Test
    void test_invalid_base64_should_throw() {

        assertThrows(IllegalArgumentException.class,
                () -> imageService.saveImage("invalid@@@", 1, "png"));
    }

    // =================================================
    // 7. TEST EXTENSION LOOP LOGIC
    // =================================================

    @Test
    void test_delete_multiple_extensions_logic() throws Exception {

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("test".getBytes());

        // tạo file với jpeg
        imageService.saveImage(base64, 20, "jpeg");

        imageService.deleteImage(20);

        assertFalse(Files.exists(Path.of("uploads/items/20.jpeg")));
    }
}