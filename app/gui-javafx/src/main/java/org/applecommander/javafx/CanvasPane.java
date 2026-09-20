package org.applecommander.javafx;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/// This is a custom region that holds the canvas and ensures the canvas is sized appropriately.
/// See [stack overflow question](https://stackoverflow.com/questions/68011270/how-to-make-canvas-fill-up-the-center-of-borderpane-in-javafx).
public class CanvasPane extends Region {
    private final Canvas canvas ;
    private Consumer<Canvas> repaint ;
    public CanvasPane() {
        this.canvas = new Canvas() ;
        getChildren().add(canvas);
        repaint = c -> {} ;
    }
    
    public Consumer<Canvas> getRepaint() {
        return repaint;
    }
    
    public void setRepaint(Consumer<Canvas> repaint) {
        this.repaint = repaint ;
    }

    public Canvas getCanvas() {
        return canvas ;
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double width = getWidth();
        canvas.setWidth(width);
        double height = getHeight();
        canvas.setHeight(height);
        repaint.accept(canvas);
    }
}