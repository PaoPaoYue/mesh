package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class RectTool implements ITool {
    private CanvasProto.CanvasPosition start;
    private CanvasProto.CanvasPosition end;
    private Color color;

    public RectTool() {
        start = null;
        end = null;
    }

    public boolean isEmpty() {
        return start == null || end == null;
    }

    @Override
    public void handleCanvasMousePressed(GraphicsContext context, MouseEvent event) {
        color = (Color) context.getStroke();
        start = CanvasProto.CanvasPosition.newBuilder()
                .setX(event.getX())
                .setY(event.getY())
                .build();
    }

    @Override
    public void handleCanvasMouseDragged(GraphicsContext context, MouseEvent event) {
        end = CanvasProto.CanvasPosition.newBuilder()
                .setX(event.getX())
                .setY(event.getY())
                .build();
        context.clearRect(0, 0, context.getCanvas().getWidth(), context.getCanvas().getHeight());
        double startX = Math.min(start.getX(), end.getX());
        double startY = Math.min(start.getY(), end.getY());
        double width = Math.abs(start.getX() - end.getX());
        double height = Math.abs(start.getY() - end.getY());
        context.fillRect(startX, startY, width, height);
    }

    @Override
    public void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void fromProto(CanvasProto.CanvasItem protoItem) {
        var item = protoItem.getRect();
        this.color = Util.fromProtoColor(item.getColor());
        this.start = item.getStart();
        this.end = item.getEnd();
    }

    @Override
    public CanvasProto.CanvasItem toProto() {
        return CanvasProto.CanvasItem.newBuilder()
                .setRect(CanvasProto.CanvasRect.newBuilder()
                        .setColor(Util.toProtoColor(color))
                        .setStart(start)
                        .setEnd(end)
                        .build())
                .build();
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setFill(color);
        double startX = Math.min(start.getX(), end.getX());
        double startY = Math.min(start.getY(), end.getY());
        double width = Math.abs(start.getX() - end.getX());
        double height = Math.abs(start.getY() - end.getY());
        context.fillRect(startX, startY, width, height);
    }
}
