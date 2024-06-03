package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class LineTool implements ITool {
    private CanvasProto.CanvasPosition start;
    private CanvasProto.CanvasPosition end;
    private Color color;
    private double lineWidth;

    public LineTool() {
        start = null;
        end = null;
    }

    public boolean isEmpty() {
        return start == null || end == null;
    }

    @Override
    public void handleCanvasMousePressed(GraphicsContext context, MouseEvent event) {
        color = (Color) context.getStroke();
        lineWidth = context.getLineWidth();
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
        context.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
    }

    @Override
    public void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event) {
    }

    @Override
    public void fromProto(CanvasProto.CanvasItem protoItem) {
        var item = protoItem.getLine();
        this.color = Util.fromProtoColor(item.getColor());
        this.lineWidth = item.getWidth();
        this.start = item.getStart();
        this.end = item.getEnd();
    }

    @Override
    public CanvasProto.CanvasItem toProto() {
        return CanvasProto.CanvasItem.newBuilder()
                .setLine(CanvasProto.CanvasLine.newBuilder()
                        .setColor(Util.toProtoColor(color))
                        .setWidth(lineWidth)
                        .setStart(start)
                        .setEnd(end)
                        .build())
                .build();
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setStroke(color);
        context.setLineWidth(lineWidth);
        context.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
    }
}
