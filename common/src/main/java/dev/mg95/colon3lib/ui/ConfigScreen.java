package dev.mg95.colon3lib.ui;

import dev.mg95.colon3lib.config.v2.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ConfigScreen extends ScrollableScreen {
    private final ConfigWrapper<?> configObject;
    private final String ID;
    private final Screen parent;

    ConfigScreen(ConfigWrapper<?> configObject, Screen parent) throws NoSuchFieldException, IllegalAccessException {
        super(Text.translatable(configObject.getClass().getDeclaredField("ID").get(configObject) + ".config.title"));

        this.configObject = configObject;
        this.ID = (String) configObject.getClass().getDeclaredField("ID").get(configObject);
        this.parent = parent;
    }

    public static ConfigScreen build(ConfigWrapper<?> config, Screen parent) {
        try {
            return new ConfigScreen(config, parent);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        super.init();
        _init();
    }

    private void _init() {
        configObject.load();

        var warningText = Text.translatable("colon3lib.config.save-warning").formatted(Formatting.ITALIC);
        var warning = new TextWidget(warningText, this.textRenderer);

        var grid = new GridWidget(1, 1); // IDK why but to move it on the x-axis I need to use a GridWidget

        warning.setX((this.getActualWidth() - this.textRenderer.getWidth(warningText.getString())) / 2);

        grid.add(warning, 0, 0);
        addWidget(grid);

        try {
            createCategory(this.configObject, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    private void createCategory(Object object, @Nullable String categoryName) throws IllegalAccessException, NoSuchFieldException {
        if (categoryName != null) {
            var categoryText = Text.translatable(ID + ".config.category." + categoryName + ".name");

            var grid = new GridWidget(1, 1); // IDK why but to move it on the x-axis I need to use a GridWidget
            var categoryLabel = new TextWidget(categoryText.formatted(Formatting.BOLD), this.textRenderer);
            categoryLabel.setX((this.getActualWidth() - this.textRenderer.getWidth(categoryText.getString())) / 2);
            grid.add(categoryLabel, 0, 0);
            addWidget(grid);
        }
        for (var field : object.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Exclude.class)) continue;

            field.setAccessible(true);
            if (field.isAnnotationPresent(Nested.class)) {
                if (categoryName == null) createCategory(field.get(object), field.getName());
                else createCategory(field.get(object), categoryName + "." + field.getName());
            } else {
                createOption(field, object, categoryName);
            }
        }
    }

    private void createOption(Field field, Object object, @Nullable String categoryName) throws IllegalAccessException, NoSuchFieldException {
        var option = field.get(object);

        var labelText = Text.translatable(ID + ".config.option." + field.getName() + ".label");
        var tooltipKey = ID + ".config.option." + field.getName() + ".tooltip";
        if (categoryName != null) {
            labelText = Text.translatable(ID + ".config.category." + categoryName + ".option." + field.getName() + ".label");
            tooltipKey = ID + ".config.category." + categoryName + ".option." + field.getName() + ".tooltip";
        }
        var label = new TextWidget(labelText, textRenderer);

        if (Language.getInstance().hasTranslation(tooltipKey)) {
            label.setTooltip(Tooltip.of(Text.translatable(tooltipKey)));
        }

        var resetButton = ButtonWidget.builder(Text.literal("↩"), button -> {
            configObject.reset(field.getName(), categoryName);

            configObject.save();
            var scrollAmount = this.bodyWidget.getScrollAmount();
            this.removeAllWidgets();
            this._init();
            this.bodyWidget.setScrollAmount(scrollAmount);
        }).build();
        resetButton.setTooltip(Tooltip.of(Text.translatable("colon3lib.config.reset-tooltip")));
        resetButton.setWidth(resetButton.getHeight());
        resetButton.setX(this.getActualWidth() - resetButton.getWidth());
        resetButton.setY(-2);

        var resetButtonOffset = resetButton.getWidth() + 2;

        var grid = new GridWidget(3, 1);
        grid.getMainPositioner().alignHorizontalCenter().alignTop();
        grid.add(label, 0, 0);

        switch (option) {
            case Boolean b -> {
                var checkboxBuilder = CheckboxWidget.builder(Text.literal(""), textRenderer);
                checkboxBuilder.checked((boolean) option);
                checkboxBuilder.callback((ignored, enabled) -> {
                    try {
                        field.set(object, enabled);
                        configObject.save();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

                var checkbox = checkboxBuilder.build();
                checkbox.setX(this.getActualWidth() - checkbox.getWidth() - resetButtonOffset + 4 /* + 4 to offset the empty message */);
                checkbox.setY(-1);

                grid.add(checkbox, 0, 1);
            }
            case String s -> {
                var textField = new TextFieldWidget(this.textRenderer, this.getActualWidth() / 4, this.getRowHeight(), Text.literal(option.toString()));
                textField.setText(option.toString());
                textField.setX(this.getActualWidth() - textField.getWidth() - resetButtonOffset);

                textField.setChangedListener((text) -> {
                    try {
                        field.set(object, text);
                        configObject.save();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

                grid.add(textField, 0, 2);
            }
            case Integer i -> {
                if (field.isAnnotationPresent(Slider.class)) {
                    var annotation = field.getAnnotation(Slider.class);
                    var value = (((Integer) option).doubleValue() - annotation.min()) / (annotation.max() - annotation.min());
                    var slider = new SliderWidget(0, 0, this.getActualWidth() / 4, this.getRowHeight(), Text.literal(option.toString()), value) {
                        @Override
                        protected void updateMessage() {
                            try {
                                this.setMessage(Text.literal(field.get(object).toString()));
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        protected void applyValue() {
                            try {
                                field.set(object, (int) ((this.value) * (annotation.max() - annotation.min()) + annotation.min()));
                                configObject.save();
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };

                    slider.setX(this.getActualWidth() - slider.getWidth() - resetButtonOffset);
                    grid.add(slider, 0, 1);
                } else {
                    var numberField = new IntegerFieldWidget(this.textRenderer, this.getActualWidth() / 4, this.getRowHeight(), Text.literal(option.toString()));
                    numberField.setText(option.toString());

                    numberField.setChangedListener((text) -> {
                        try {
                            try {
                                field.set(object, Integer.valueOf(text));
                            } catch (NumberFormatException e) {
                                field.set(object, 0);
                            }
                            configObject.save();
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    numberField.setX(this.getActualWidth() - numberField.getWidth() - resetButtonOffset);
                    grid.add(numberField, 0, 1);
                }
            }
            case Double d -> {
                if (field.isAnnotationPresent(DoubleSlider.class)) {
                    var annotation = field.getAnnotation(DoubleSlider.class);
                    var value = ((double) option - annotation.min()) / (annotation.max() - annotation.min());
                    var slider = new SliderWidget(0, 0, this.getActualWidth() / 4, this.getRowHeight(), Text.literal(option.toString()), value) {
                        @Override
                        protected void updateMessage() {
                            try {
                                this.setMessage(Text.literal(field.get(object).toString()));
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        protected void applyValue() {
                            try {
                                field.set(object, Math.round((((this.value) * (annotation.max() - annotation.min()) + annotation.min())) * 10.0) / 10.0);
                                configObject.save();
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };

                    slider.setX(this.getActualWidth() - slider.getWidth() - resetButtonOffset);
                    grid.add(slider, 0, 1);
                } else {
                    var numberField = new DoubleFieldWidget(this.textRenderer, this.getActualWidth() / 4, this.getRowHeight(), Text.literal(option.toString()));
                    numberField.setText(option.toString());

                    numberField.setChangedListener((text) -> {
                        try {
                            try {
                                field.set(object, Double.valueOf(text));
                            } catch (NumberFormatException e) {
                                field.set(object, 0.0);
                            }
                            configObject.save();
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    numberField.setX(this.getActualWidth() - numberField.getWidth() - resetButtonOffset);
                    grid.add(numberField, 0, 1);
                }
            }

            case null, default -> throw new TypeNotSupportedException(option.getClass());
        }
        grid.add(resetButton, 0, 2);

        addWidget(grid);


    }

    @Override
    public void done(ButtonWidget button) {
        configObject.save();
        super.done(button);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
