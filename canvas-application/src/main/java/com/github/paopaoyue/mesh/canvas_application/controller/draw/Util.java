package com.github.paopaoyue.mesh.canvas_application.controller.draw;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;

public class Util {
    public static CanvasProto.CanvasColor toProtoColor(javafx.scene.paint.Color color) {
        return CanvasProto.CanvasColor.newBuilder()
                .setR(color.getRed())
                .setG(color.getGreen())
                .setB(color.getBlue())
                .build();
    }

    public static javafx.scene.paint.Color fromProtoColor(CanvasProto.CanvasColor color) {
        return javafx.scene.paint.Color.color(color.getR(), color.getG(), color.getB());
    }
}
