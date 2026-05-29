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
import java.time.ZoneId;

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
        startDatePicker.setValue(LocalDate.now());

        uploadImageButton.setOnAction(event -> handleUploadImage());
        removeImageButton.setOnAction(event -> handleRemoveImage());
        saveButton.setOnAction(event -> handleSaveItem());
    }

    /**
     * Cấu hình chế độ form (Thêm mới/Sửa đổi).
     */
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

    /**
     * Gửi thông tin sản phẩm thô lên Server thông qua NetworkService
     */
    private void handleSaveItem() {
        errorLabel.setText("");

        if (ValidationUtil.isEmpty(itemNameField.getText()) || categoryCombo.getValue() == null || ValidationUtil.isEmpty(startingPriceField.getText())) {
            errorLabel.setText("Vui lòng điền đầy đủ thông tin bắt buộc!");
            return;
        }

        if (!ValidationUtil.isNumber(startingPriceField.getText())) {
            errorLabel.setText("Giá khởi điểm phải là chữ số!");
            return;
        }

        LocalDateTime startDateTime = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(startHourSpinner.getValue(), 0));
        LocalDateTime endDateTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(endHourSpinner.getValue(), 0));
        long startTimeMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTimeMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (!ValidationUtil.isValidAuctionDuration(startTimeMillis, endTimeMillis)) {
            errorLabel.setText("Ngày kết thúc phải nằm ở tương lai và sau ngày bắt đầu!");
            return;
        }

        try {
            JsonObject itemJson = new JsonObject();
            itemJson.addProperty("isEditMode", isEditMode);
            if (isEditMode) itemJson.addProperty("itemId", editingItemId);
            itemJson.addProperty("title", itemNameField.getText().trim());
            itemJson.addProperty("description", descriptionArea.getText().trim());
            itemJson.addProperty("category", categoryCombo.getValue().name());
            itemJson.addProperty("reservePrice", Double.parseDouble(startingPriceField.getText()));
            itemJson.addProperty("startTime", startTimeMillis);
            itemJson.addProperty("endTime", endTimeMillis);

            // ĐÃ SỬA: Xóa khối outStream cũ, sử dụng NetworkService để gửi gói tin JSON
            NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, itemJson.toString());
            NetworkService.getInstance().sendNetworkMessage(message);

            DialogUtil.showInfo("Đã gửi yêu cầu lưu sản phẩm lên hệ thống Máy chủ.");

            // Đóng cửa sổ sau khi gửi thành công
            if (saveButton.getScene() != null && saveButton.getScene().getWindow() != null) {
                ((Stage) saveButton.getScene().getWindow()).close();
            }

        } catch (Exception e) {
            LoggerUtil.error("Sự cố truyền tin Socket tạo sản phẩm.", e);
        }
    }
}