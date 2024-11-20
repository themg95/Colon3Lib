package dev.mg95.colon3lib.config.v2;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.Set;

@SupportedAnnotationTypes("dev.mg95.colon3lib.config.v2.ConfigModel")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ConfigProcessor extends AbstractProcessor {
    private static final String TEMPLATE = """
            package {package};
            
            import dev.mg95.colon3lib.config.v2.ConfigWrapper;
            import dev.mg95.colon3lib.config.v2.Exclude;
            import dev.mg95.colon3lib.config.v2.Slider;
            import dev.mg95.colon3lib.config.v2.DoubleSlider;
            import dev.mg95.colon3lib.config.v2.Nested;
            
            public class {name} extends ConfigWrapper<{model}> {
                @Exclude
                public static final String ID = "{id}";
            
            {fields}
            
            {methods}
            
            {nested}
            
                public {name}() {
                    init(this, ID, {model}.class);
                }
            }
            """;

    private static final String NESTED_TEMPLATE = """
            @Nested
            public {name} {varName} = new {name}(this);
            
            public static class {name} extends ConfigWrapper<{model}> {
                @Exclude
                public Object outer;
            
                public {name}(Object outer) {
                    this.outer = outer;
                    init(this, {model}.class, this.outer);
                }
            
            {fields}
            
            {methods}
            
            {nested}
            }
            """;

    private static final String GETTER_TEMPLATE = """
                public {type} {name}() { return {name}; }
            """;
    private static final String SETTER_TEMPLATE = """
                public {type} {name}({type} newValue) { return {name} = newValue; }
            """;


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            writeFile(annotations, roundEnv);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private void writeFile(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws Exception {
        for (var element : roundEnv.getElementsAnnotatedWith(ConfigModel.class)) {
            var name = element.getAnnotation(ConfigModel.class).name();
            var fullName = ((TypeElement) element).getQualifiedName().toString();
            var packageName = fullName.substring(0, fullName.lastIndexOf('.'));

            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(name);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                out.println(TEMPLATE
                        .replace("{package}", packageName)
                        .replace("{name}", name)
                        .replace("{model}", ((TypeElement) element).getQualifiedName())
                        .replace("{id}", element.getAnnotation(ConfigModel.class).id())
                        .replace("{fields}", getFields(element, fullName))
                        .replace("{methods}", getMethods(element, fullName))
                        .replace("{nested}", getNested(element, fullName))
                );
            }
        }

    }

    private String getMethods(Element element, String fullName) {
        StringBuilder out = new StringBuilder();

        for (VariableElement enclosedElement : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            if (!enclosedElement.getKind().equals(ElementKind.FIELD)) continue;
            out.append(getMethodStrings(enclosedElement.asType().toString(), enclosedElement.getSimpleName().toString()));
        }

        return out.toString();
    }

    private String getFields(Element element, String fullName) {
        StringBuilder out = new StringBuilder();
        for (VariableElement enclosedElement : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            if (!enclosedElement.getKind().equals(ElementKind.FIELD)) continue;

            if (enclosedElement.getAnnotation(Exclude.class) != null) out.append("    @Exclude\n");
            if (enclosedElement.getAnnotation(Slider.class) != null) {
                var annotation = enclosedElement.getAnnotation(Slider.class);
                out.append("    @Slider(min = " + annotation.min() + ", max = " + annotation.max() + ")\n");
            }
            if (enclosedElement.getAnnotation(DoubleSlider.class) != null) {
                var annotation = enclosedElement.getAnnotation(DoubleSlider.class);
                out.append("    @DoubleSlider(min = " + annotation.min() + ", max = " + annotation.max() + ")\n");
            }

            out.append(String.format("    private static %s %s;\n",
                    enclosedElement.asType(),
                    enclosedElement.getSimpleName()
            ));
        }

        return out.toString();
    }

    private String getNested(Element element, String fullName) {
        StringBuilder out = new StringBuilder();

        for (var nestedElement : element.getEnclosedElements()) {
            if (nestedElement.getAnnotation(Nested.class) == null) continue;

            var name = nestedElement.getSimpleName().toString();
            var varName = camelCase(nestedElement.getSimpleName().toString());

            out.append(NESTED_TEMPLATE
                    .replace("{name}", name)
                    .replace("{varName}", varName)
                    .replace("{model}", ((TypeElement) nestedElement).getQualifiedName())
                    //.replace("{etters}", getMethodStrings(name, varName))
                    .replace("{fields}", getFields(nestedElement, fullName))
                    .replace("{methods}", getMethods(nestedElement, fullName))
                    .replace("{nested}", getNested(nestedElement, fullName))
            );
        }
        return out.toString();
    }

    private static String camelCase(String in) {
        return in.substring(0, 1).toLowerCase() + in.substring(1);
    }

    private static String getMethodStrings(String type, String name) {
        StringBuilder out = new StringBuilder();

        out.append(GETTER_TEMPLATE
                .replace("{type}", type)
                .replace("{name}", name)
        );

        out.append(SETTER_TEMPLATE
                .replace("{type}", type)
                .replace("{name}", name)
        );

        return out.toString();
    }
}
