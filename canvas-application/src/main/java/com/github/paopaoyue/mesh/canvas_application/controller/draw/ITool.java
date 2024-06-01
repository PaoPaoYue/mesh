package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public interface ITool {
    boolean isEmpty();

    void handleCanvasMousePressed(GraphicsContext context, MouseEvent event);

    void handleCanvasMouseDragged(GraphicsContext context, MouseEvent event);

    void handleCanvasMouseReleased(GraphicsContext context, MouseEvent event);

    void fromProto(CanvasProto.CanvasItem protoItem);

    CanvasProto.CanvasItem toProto();

    void draw(GraphicsContext context);
}
