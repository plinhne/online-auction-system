package com.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final String UPLOAD_DIR = "uploads/items/";

    public ImageService() {
        new File(UPLOAD_DIR).mkdirs();
    }

    /**
     * Nhận base64 string, lưu file, trả về relative path.
     */
    public String saveImage(String base64Data, int itemId, String extension) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        String filename = itemId + "." + extension;
        Path path = Path.of(UPLOAD_DIR + filename);
        Files.write(path, bytes);
        logger.info("Image saved: {}", path);
        return UPLOAD_DIR + filename;
    }

    public void deleteImage(int itemId) {
        for (String ext : new String[]{"jpg", "jpeg", "png"}) {
            File file = new File(UPLOAD_DIR + itemId + "." + ext);
            if (file.exists()) {
                file.delete();
                logger.info("Image deleted: {}", file.getPath());
                return;
            }
        }
    }
}