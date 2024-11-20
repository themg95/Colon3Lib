package dev.mg95.colon3lib.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.Nullable;

public class DoubleFieldWidget extends TextFieldWidget {

    public DoubleFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
    }

    public DoubleFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
    }

    public DoubleFieldWidget(TextRenderer textRenderer, int width, int height, Text text) {
        super(textRenderer, width, height, text);
    }

    @Override
    public void write(String text) {
        for (var chr : text.toCharArray()) {
            if (!Character.isDigit(chr) && chr != '.') return;
            if (this.getText().contains(".") && chr == '.') return;
        }
        super.write(text);
    }

}
