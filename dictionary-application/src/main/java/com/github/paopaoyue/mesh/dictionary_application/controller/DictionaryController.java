package com.github.paopaoyue.mesh.dictionary_application.controller;

import com.github.paopaoyue.mesh.dictionary_application.api.IDictionaryCaller;
import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnBean(IDictionaryCaller.class)
public class DictionaryController {

    private IDictionaryCaller caller;

    @FXML
    private TextField keyFieldAdd;
    @FXML
    private TextField valueFieldAdd;
    @FXML
    private TextField keyFieldQuery;
    @FXML
    private TextArea valueQueryResult;
    @FXML
    private TextField keyFieldUpdate;
    @FXML
    private TextField valueFieldUpdate;
    @FXML
    private TextField keyFieldRemove;

    @FXML
    private Label addOutput;
    @FXML
    private Label queryOutput;
    @FXML
    private Label updateOutput;
    @FXML
    private Label removeOutput;

    public DictionaryController(IDictionaryCaller caller) {
        this.caller = caller;
    }

    public void addEntry(ActionEvent actionEvent) {
        String key = keyFieldAdd.getText();
        String value = valueFieldAdd.getText();
        if (!key.isEmpty() && !value.isEmpty()) {
            Instant start = Instant.now();
            Dictionary.AddResponse response = caller.add(Dictionary.AddRequest.newBuilder().setKey(key).setValue(value).build(), new CallOption());
            Instant end = Instant.now();
            if (RespBaseUtil.isOK(response.getBase())) {
                addOutput.setTextFill(Color.GREEN);
                addOutput.setText("Add operation success, total time cost: " + Duration.between(start, end).toMillis() + " ms");
                addOutput.setVisible(true);
            } else {
                addOutput.setTextFill(javafx.scene.paint.Color.RED);
                addOutput.setText(response.getBase().getMessage());
                addOutput.setVisible(true);
            }
        } else {
            addOutput.setTextFill(javafx.scene.paint.Color.RED);
            addOutput.setText("Key or Value is empty");
            addOutput.setVisible(true);
        }
    }

    public void queryEntry(ActionEvent actionEvent) {
        String key = keyFieldQuery.getText();
        if (!key.isEmpty()) {
            Instant start = Instant.now();
            Dictionary.GetResponse response = caller.get(Dictionary.GetRequest.newBuilder().setKey(key).build(), new CallOption());
            Instant end = Instant.now();
            if (RespBaseUtil.isOK(response.getBase())) {
                queryOutput.setTextFill(Color.GREEN);
                queryOutput.setText("Query operation success, total time cost: " + Duration.between(start, end).toMillis() + " ms");
                valueQueryResult.setText(response.getValue());
                queryOutput.setVisible(true);
            } else {
                queryOutput.setTextFill(Color.RED);
                queryOutput.setText(response.getBase().getMessage());
                queryOutput.setVisible(true);
            }
        } else {
            queryOutput.setTextFill(Color.RED);
            queryOutput.setText("Key is empty");
            queryOutput.setVisible(true);
        }
    }

    public void updateEntry(ActionEvent actionEvent) {
        String key = keyFieldUpdate.getText();
        String value = valueFieldUpdate.getText();
        if (!key.isEmpty() && !value.isEmpty()) {
            Instant start = Instant.now();
            Dictionary.UpdateResponse response = caller.update(Dictionary.UpdateRequest.newBuilder().setKey(key).build(), new CallOption());
            Instant end = Instant.now();
            if (RespBaseUtil.isOK(response.getBase())) {
                response = caller.update(Dictionary.UpdateRequest.newBuilder().setKey(key).setValue(value).build(), new CallOption());
                if (RespBaseUtil.isOK(response.getBase())) {
                    updateOutput.setTextFill(Color.GREEN);
                    updateOutput.setText("Update operation success, total time cost: " + Duration.between(start, end).toMillis() + " ms");
                    updateOutput.setVisible(true);
                } else {
                    updateOutput.setTextFill(Color.RED);
                    updateOutput.setText(response.getBase().getMessage());
                    updateOutput.setVisible(true);
                }
            } else {
                updateOutput.setTextFill(Color.RED);
                updateOutput.setText(response.getBase().getMessage());
                updateOutput.setVisible(true);
            }
        } else {
            updateOutput.setTextFill(Color.RED);
            updateOutput.setText("Key or Value is empty");
            updateOutput.setVisible(true);
        }
    }

    public void removeEntry(ActionEvent actionEvent) {
        String key = keyFieldRemove.getText();
        if (!key.isEmpty()) {
            Instant start = Instant.now();
            Dictionary.RemoveResponse response = caller.remove(Dictionary.RemoveRequest.newBuilder().setKey(key).build(), new CallOption());
            Instant end = Instant.now();
            if (RespBaseUtil.isOK(response.getBase())) {
                removeOutput.setTextFill(Color.GREEN);
                removeOutput.setText("Remove operation success, total time cost: " + Duration.between(start, end).toMillis() + " ms");
                removeOutput.setVisible(true);
            } else {
                removeOutput.setTextFill(Color.RED);
                removeOutput.setText(response.getBase().getMessage());
                removeOutput.setVisible(true);
            }
        } else {
            removeOutput.setTextFill(Color.RED);
            removeOutput.setText("Key is empty");
            removeOutput.setVisible(true);
        }
    }

}
