package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class CircleTool implements ITool {
    private CanvasProto.CanvasPosition pos;
    private double radius;
    private Color color;

    public CircleTool() {
        pos = null;
    }

    public boolean isEmpty() {
        return pos == null;
    }

    @Override
    public void handleCanvasMousePressed(GraphicsContext context, MouseEvent event) {
        color = (Color) context.getStroke();
        pos = CanvasProto.CanvasPosition.newBuilder()
                .setX(event.getX())
                .setY(event.getY())
                .build();
    }

    @Override
    public void handleCanvasMouseDragged(GraphicsContext context, MouseEvent event) {
        radius = Math.sqrt(Math.pow(event.getX() - pos.getX(), 2) + Math.pow(event.getY() - pos.getY(), 2));
        context.clearRect(0, 0, context.getCanvas().getWidth(), context.getCanvas().getHeight());
        context.fillOval(pos.getX() - radius, pos.getY() - radius, radius * 2, radius * 2);
    }

    @Override
    public void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void fromProto(CanvasProto.CanvasItem protoItem) {
        var item = protoItem.getCircle();
        this.color = Util.fromProtoColor(item.getColor());
        this.pos = item.getPos();
        this.radius = item.getRadius();
    }

    @Override
    public CanvasProto.CanvasItem toProto() {
        return CanvasProto.CanvasItem.newBuilder()
                .setCircle(CanvasProto.CanvasCircle.newBuilder()
                        .setColor(Util.toProtoColor(color))
                        .setPos(pos)
                        .setRadius(radius)
                        .build())
                .build();
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setFill(color);
        context.fillOval(pos.getX() - radius, pos.getY() - radius, radius * 2, radius * 2);
    }
}
