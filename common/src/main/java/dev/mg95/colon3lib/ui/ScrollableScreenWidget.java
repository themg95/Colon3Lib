package dev.mg95.colon3lib.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public class ScrollableScreenWidget extends ElementListWidget<ScrollableScreenWidget.Entry> {

    public ScrollableScreenWidget(MinecraftClient client, int width, int height, int y, int rowHeight) {
        super(client, width, height, y, rowHeight);
    }

    public void add(Widget widget) {
        addEntry(new Entry(widget));
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    public int getRowHeight() {
        return this.itemHeight - 4;
    }

    static class Entry extends ElementListWidget.Entry<Entry> {
        private final Widget widget;
        private final List<ClickableWidget> children = new ArrayList<>();

        public Entry(Widget widget) {
            this.widget = widget;
            widget.forEachChild(children::add);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return this.children;
        }

        @Override
        public List<? extends Element> children() {
            return this.children;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.widget.setPosition(x, y);
            this.widget.forEachChild(child -> child.render(context, mouseX, mouseY, tickDelta));
        }
    }
}
