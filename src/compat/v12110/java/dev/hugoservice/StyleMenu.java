package dev.hugoservice;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public class StyleMenu extends StyleScreen {
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        return this.hsMouseClicked(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (this.hsMouseDragged(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.hsMouseReleased();
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.hsKeyPressed(input.key(), input.scancode(), input.modifiers(), Mc.hasCtrl(input))) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.hsCharTyped()) {
            return true;
        }
        return super.charTyped(input);
    }
}
