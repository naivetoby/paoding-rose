package net.paoding.rose.jade.scan;

import net.paoding.rose.jade.annotation.DAO;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.processing.Completion;
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
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 编译期 扫描DAO注解,生成rose.jade.%s.scanner<br/>
 * 模仿spring-context-indexer生成索引文件
 * @author zhangfusheng
 * @date 2023/6/28
 */
@SupportedAnnotationTypes("net.paoding.rose.jade.annotation.DAO")
public class RoseJadeScaner implements Processor {

    static final String SCANNER_PATH = "META-INF/rose.jade.%s.scanner";
    static AtomicInteger VERSION = new AtomicInteger(0);

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
        List<String> daoClass = elementsAnnotatedWithDao.stream()
                .filter(element -> Objects.nonNull(element.getAnnotation(DAO.class)))
                .map(Object::toString).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(daoClass)) return false;
        try {
            FileObject fileObject = this.processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", String.format(SCANNER_PATH, VERSION.getAndIncrement()));
            try (OutputStream outputStream = fileObject.openOutputStream()) {
                daoClass.forEach(element -> {
                    try {
                        outputStream.write((element + System.lineSeparator()).getBytes(StandardCharsets.ISO_8859_1));
                    } catch (IOException e) {
                        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                });
            }
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
