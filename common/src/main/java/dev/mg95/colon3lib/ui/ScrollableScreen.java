package dev.mg95.colon3lib.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;

import java.awt.*;

public class ScrollableScreen extends Screen {
    protected ScrollableScreenWidget bodyWidget;
    protected ButtonWidget doneButton;

    public ScrollableScreen(Text title) {
        super(title);
    }

    public void addWidget(Widget widget) {
        bodyWidget.add(widget);
    }

    public void removeAllWidgets() {
        var scrollAmount = bodyWidget.getScrollAmount();
        this.remove(bodyWidget);
        createBodyWidget();
        this.bodyWidget.setScrollAmount(scrollAmount);
    }

    public int getActualWidth() {
        return this.bodyWidget.getRowWidth();
    }

    public int getActualHeight() {
        return this.bodyWidget.getHeight();
    }

    public int getRowHeight() {
        return this.bodyWidget.getRowHeight();
    }


    @Override
    public void init() {
        super.init();

        this.doneButton = ButtonWidget.builder(Text.translatable("gui.done"), this::done).build();
        this.doneButton.setX((this.width - doneButton.getWidth()) / 2);
        this.doneButton.setY(this.height - (textRenderer.fontHeight * 1 + doneButton.getHeight()));
        this.addDrawableChild(doneButton);

        createBodyWidget();
    }

    private void createBodyWidget() {
        this.bodyWidget = new ScrollableScreenWidget(this.client, this.width, this.height - ((textRenderer.fontHeight * 5) + this.doneButton.getHeight()), textRenderer.fontHeight * 3, 20);
        this.addDrawableChild(bodyWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.client.textRenderer, this.title, this.width / 2, textRenderer.fontHeight, 0xFFFFFF);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public void done(ButtonWidget button) {
        this.close();
    }

}
