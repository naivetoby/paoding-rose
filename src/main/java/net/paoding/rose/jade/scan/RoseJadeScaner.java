package net.paoding.rose.jade.scan;

import net.paoding.rose.jade.annotation.DAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 编译期 扫描DAO注解,生成rose.jade.%s.scanner<br/>
 * 模仿spring-context-indexer生成索引文件
 * @author zhangfusheng
 * @date 2023/6/28
 */
@SupportedAnnotationTypes("net.paoding.rose.jade.annotation.DAO")
public class RoseJadeScaner implements Processor {

    static final String SCANNER_PATH = "META-INF/rose.jade.scanner";
    static final Set<String> DAO_CLASS_LIST = new HashSet<>();

    private ProcessingEnvironment processingEnv;

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        Filer filer = this.processingEnv.getFiler();
        try (InputStream inputStream = filer.getResource(StandardLocation.CLASS_OUTPUT, "", SCANNER_PATH).openInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String readLine = bufferedReader.readLine();
            while (StringUtils.isNotBlank(readLine)) {
                DAO_CLASS_LIST.add(readLine);
                readLine = bufferedReader.readLine();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Class<?> aClass = this.getClass().getClassLoader().loadClass("net.paoding.rose.jade.annotation.DAO");
            if (Objects.isNull(aClass)) return false;
        } catch (Exception e) {
            return false;
        }
        Set<? extends Element> elementsAnnotatedWithDao = roundEnv.getElementsAnnotatedWith(DAO.class);
        if (CollectionUtils.isEmpty(elementsAnnotatedWithDao)) return false;
        elementsAnnotatedWithDao.stream().map(Object::toString).forEach(DAO_CLASS_LIST::add);
        try (OutputStream outputStream = this.processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", SCANNER_PATH).openOutputStream()) {
            DAO_CLASS_LIST.forEach(element -> {
                try {
                    outputStream.write((element + System.lineSeparator()).getBytes(StandardCharsets.ISO_8859_1));
                } catch (IOException e) {
                    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                }
            });
        } catch (IOException e) {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptyList();
    }
}
