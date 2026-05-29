package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

public class AdminPanelController extends BaseController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private Button changeBalanceButton;

    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ... (Các cột khác)
        balanceColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0fđ", data.getValue().getWalletBalance())));

        if (changeBalanceButton != null) {
            changeBalanceButton.setOnAction(event -> handleChangeBalance());
        }
    }

    private void handleChangeBalance() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn người dùng!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getWalletBalance()));
        dialog.setTitle("Quản lý Ví");
        dialog.setContentText("Nhập số dư mới:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double newBalance = Double.parseDouble(input.trim());
                JsonObject payload = new JsonObject();
                payload.addProperty("adminAction", "CHANGE_BALANCE");
                payload.addProperty("targetId", selected.getId());
                payload.addProperty("newBalance", newBalance);

                // Gửi qua NetworkService thay vì outStream
                NetworkMessage message = new NetworkMessage(MessageType.ADMIN_ACTION_REQUEST, payload.toString());
                NetworkService.getInstance().sendNetworkMessage(message);

                selected.setWalletBalance(newBalance);
                usersTable.refresh();
            } catch (NumberFormatException e) {
                DialogUtil.showWarning("Chỉ nhập số!");
            }
        });
    }
}