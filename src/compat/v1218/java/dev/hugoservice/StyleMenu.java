package dev.hugoservice;

public class StyleMenu extends StyleScreen {
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) {
            return true;
        }
        return this.hsMouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (this.hsMouseDragged(x, y, button)) {
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        this.hsMouseReleased();
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        boolean ctrl = (modifiers & 2) != 0 || (modifiers & 8) != 0;
        if (this.hsKeyPressed(key, scancode, modifiers, ctrl)) {
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.hsCharTyped()) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
