package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.URLDecoder;
import java.net.URLEncoder;

public class TextTool implements ITool {
    private static final double DEFAULT_FONT_SIZE = 14;

    private CanvasProto.CanvasPosition pos;
    private String text;
    private Color color;
    private double fontSize = DEFAULT_FONT_SIZE; // use default font size as for now

    public TextTool() {
        pos = null;
        text = "";
    }

    public boolean isEmpty() {
        return pos == null || text.isEmpty();
    }

    public void handleCanvasTextInput(GraphicsContext context, Color color, String text, double x, double y) {
        this.color = color;
        this.pos = CanvasProto.CanvasPosition.newBuilder()
                .setX(x)
                .setY(y)
                .build();
        this.text = text;
        context.setFont(Font.font(fontSize));
        context.fillText(text, pos.getX(), pos.getY());
    }

    @Override
    public void handleCanvasMousePressed(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void handleCanvasMouseDragged(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void fromProto(CanvasProto.CanvasItem protoItem) {
        var item = protoItem.getText();
        this.color = Util.fromProtoColor(item.getColor());
        this.pos = item.getPos();
        this.text = URLDecoder.decode(item.getText(), java.nio.charset.StandardCharsets.UTF_8);
        this.fontSize = item.getFontSize();
    }

    @Override
    public CanvasProto.CanvasItem toProto() {
        return CanvasProto.CanvasItem.newBuilder()
                .setText(CanvasProto.CanvasText.newBuilder()
                        .setColor(Util.toProtoColor(color))
                        .setPos(pos)
                        .setText(URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8))
                        .setFontSize(fontSize)
                        .build())
                .build();
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setStroke(color);
        context.setFont(Font.font(fontSize));
        context.fillText(text, pos.getX(), pos.getY());
    }
}
