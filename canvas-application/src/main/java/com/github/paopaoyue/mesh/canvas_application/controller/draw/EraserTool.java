package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EraserTool implements ITool {
    private final List<CanvasProto.CanvasPosition> positions;
    private double lineWidth;
    private Color tempColor;

    public EraserTool() {
        positions = new CopyOnWriteArrayList<>();
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }

    @Override
    public void handleCanvasMousePressed(GraphicsContext context, MouseEvent event) {
        tempColor = (Color) context.getStroke();
        lineWidth = context.getLineWidth();
        context.setStroke(Color.WHITE);
        context.beginPath();
        context.moveTo(event.getX(), event.getY());
        positions.add(CanvasProto.CanvasPosition.newBuilder()
                .setX(event.getX())
                .setY(event.getY())
                .build());
    }

    @Override
    public void handleCanvasMouseDragged(GraphicsContext context, MouseEvent event) {
        context.lineTo(event.getX(), event.getY());
        context.stroke();
        positions.add(CanvasProto.CanvasPosition.newBuilder()
                .setX(event.getX())
                .setY(event.getY())
                .build());
    }

    @Override
    public void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event) {
        context.closePath();
        context.setStroke(tempColor);
    }

    @Override
    public void fromProto(CanvasProto.CanvasItem protoItem) {
        var item = protoItem.getEraser();
        this.lineWidth = item.getWidth();
        this.positions.addAll(item.getPositionsList());
    }

    @Override
    public CanvasProto.CanvasItem toProto() {
        return CanvasProto.CanvasItem.newBuilder()
                .setEraser(CanvasProto.CanvasEraser.newBuilder()
                        .setWidth(lineWidth)
                        .addAllPositions(positions)
                        .build())
                .build();
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setStroke(Color.WHITE);
        context.setLineWidth(lineWidth);
        context.beginPath();
        context.moveTo(positions.getFirst().getX(), positions.getFirst().getY());
        for (var pos : positions) {
            context.lineTo(pos.getX(), pos.getY());
            context.stroke();
        }
        context.closePath();
    }
}
