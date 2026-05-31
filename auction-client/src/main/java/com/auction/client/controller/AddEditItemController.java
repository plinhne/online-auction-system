package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.item.ItemCategory;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.JsonObject;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Bổ sung các thư viện để xử lý Base64 và File
import java.nio.file.Files;
import java.util.Base64;
import java.io.IOException;

public class AddEditItemController extends BaseController {

    @FXML private Label titleLabel;
    @FXML private TextField itemNameField;
    @FXML private TextArea descriptionArea;
    @FXML private ImageView imagePreview;
    @FXML private Button uploadImageButton;
    @FXML private Button removeImageButton;
    @FXML private ComboBox<ItemCategory> categoryCombo;
    @FXML private TextField startingPriceField;
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private DatePicker endDatePicker;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private File selectedImageFile;
    private boolean isEditMode = false;
    private int editingItemId = -1;

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(ItemCategory.values()));
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18));

        // Mặc định ngày bắt đầu là hôm nay, kết thúc là ngày mai
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        uploadImageButton.setOnAction(event -> handleUploadImage());
        removeImageButton.setOnAction(event -> handleRemoveImage());
        saveButton.setOnAction(event -> handleSaveItem());
        cancelButton.setOnAction(event -> closeWindow());
    }

    public void setFormMode(boolean isEditMode, int itemId) {
        this.isEditMode = isEditMode;
        this.editingItemId = itemId;
        if (isEditMode) {
            titleLabel.setText("Cập nhật thông tin sản phẩm");
            saveButton.setText("Cập nhật sản phẩm");
        }
    }

    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fileChooser.showOpenDialog((Stage) uploadImageButton.getScene().getWindow());
        if (selectedImageFile != null) {
            imagePreview.setImage(new Image(selectedImageFile.toURI().toString()));
            removeImageButton.setVisible(true);
        }
    }

    private void handleRemoveImage() {
        selectedImageFile = null;
        imagePreview.setImage(null);
        removeImageButton.setVisible(false);
    }

    @FXML
    private void handleSaveItem() {
        errorLabel.setText("");

        if (ValidationUtil.isEmpty(itemNameField.getText()) || categoryCombo.getValue() == null || ValidationUtil.isEmpty(startingPriceField.getText())) {
            errorLabel.setText("Vui lòng điền đầy đủ thông tin bắt buộc!");
            return;
        }

        if (!ValidationUtil.isNumber(startingPriceField.getText())) {
            errorLabel.setText("Giá khởi điểm phải là chữ số hợp lệ!");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            errorLabel.setText("Vui lòng chọn ngày bắt đầu và kết thúc!");
            return;
        }

        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.of(startHourSpinner.getValue(), 0));
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.of(endHourSpinner.getValue(), 0));

        if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
            errorLabel.setText("Ngày kết thúc phải nằm ở tương lai và sau ngày bắt đầu!");
            return;
        }

        try {
            JsonObject itemJson = new JsonObject();
            if (isEditMode) {
                itemJson.addProperty("itemId", editingItemId);
            }

            // 1. Các trường dữ liệu dành cho ItemController tạo Item
            itemJson.addProperty("name", itemNameField.getText().trim());
            itemJson.addProperty("description", descriptionArea.getText().trim());
            itemJson.addProperty("category", categoryCombo.getValue().name());
            itemJson.addProperty("startingPrice", Double.parseDouble(startingPriceField.getText()));

            // 2. Các trường dữ liệu dành cho AuctionController tạo phòng đấu giá
            itemJson.addProperty("startTime", startDateTime.toString());
            itemJson.addProperty("endTime", endDateTime.toString());
            itemJson.addProperty("minIncrement", 1000.0);

            // 3. Xử lý ảnh: Chuyển đổi File sang chuỗi Base64
            if (selectedImageFile != null) {
                try {
                    // Đọc toàn bộ byte của file ảnh
                    byte[] fileContent = java.nio.file.Files.readAllBytes(selectedImageFile.toPath());
                    // Chuyển đổi mảng byte sang chuỗi Base64
                    String encodedString = java.util.Base64.getEncoder().encodeToString(fileContent);
                    itemJson.addProperty("imageBase64", encodedString);

                    // Trích xuất đuôi mở rộng của file ảnh (ví dụ: jpg, png)
                    String fileName = selectedImageFile.getName();
                    String extension = "";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) {
                        extension = fileName.substring(i + 1);
                    }
                    itemJson.addProperty("imageExtension", extension);

                } catch (java.io.IOException ex) {
                    LoggerUtil.error("Lỗi đọc file ảnh khi lưu", ex);
                    errorLabel.setText("Lỗi xử lý hình ảnh. Vui lòng chọn lại ảnh!");
                    return; // Ngừng quá trình gửi tin nếu có lỗi khi đọc file ảnh
                }
            }

            // ==============================================================
            // PHẦN BỔ SUNG QUAN TRỌNG: GỬI GÓI TIN LÊN SERVER VÀ ĐÓNG CỬA SỔ
            // ==============================================================

            NetworkMessage message;
            if (isEditMode) {
                // Đang ở chế độ sửa => gửi EDIT_ITEM_REQUEST
                message = new NetworkMessage(MessageType.EDIT_ITEM_REQUEST, itemJson.toString());
                LoggerUtil.info("Đang gửi yêu cầu Sửa Sản phẩm (ID: " + editingItemId + ") lên Server...");
            } else {
                // Đang ở chế độ thêm mới => gửi ADD_ITEM_REQUEST
                // Lưu ý: Server đã được cấu hình tự sinh CREATE_AUCTION_RESPONSE sau khi nhận ADD_ITEM_REQUEST
                message = new NetworkMessage(MessageType.ADD_ITEM_REQUEST, itemJson.toString());
                LoggerUtil.info("Đang gửi yêu cầu Thêm Sản phẩm & Mở Phiên đấu giá lên Server...");
            }

            // 4. Phát lệnh đi thông qua Socket
            NetworkService.getInstance().sendNetworkMessage(message);

            // 5. Đóng cửa sổ Popup AddItemView lại để về màn hình Dashboard
            Stage stage = (Stage) itemNameField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            LoggerUtil.error("Lỗi không xác định khi lưu thông tin sản phẩm", e);
            errorLabel.setText("Lỗi hệ thống: Không thể xử lý dữ liệu.");
        }
    }

    private void closeWindow() {
        if (saveButton.getScene() != null && saveButton.getScene().getWindow() != null) {
            saveButton.getScene().getWindow().hide();        }
    }
}