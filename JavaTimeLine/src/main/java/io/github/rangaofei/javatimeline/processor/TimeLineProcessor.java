package io.github.rangaofei.javatimeline.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import io.github.rangaofei.javatimeline.AdapterUtil;
import io.github.rangaofei.javatimeline.TimeLineContext;
import io.github.rangaofei.javatimeline.annotations.TimeLine;
import io.github.rangaofei.javatimeline.viewattr.ImageViewAttr;
import io.github.rangaofei.javatimeline.viewattr.TextViewAttr;

public class TimeLineProcessor implements TimeLineProcess {
    private Element element;
    private String adapterName;
    private String packageName;
    private String className;
    private String fullClassName;
    private String keyLayoutId;
    private String valueLayoutId;
    private MethodSpec getKeyLayoutIdMethod;
    private MethodSpec getValueLayoutIdMethod;
    private MethodSpec bindKeyItemMethod;
    private MethodSpec bindValueItemMethod;
    private MethodSpec constructorMethod;

    public TimeLineProcessor(Element element) {
        this.element = element;
    }

    @Override
    public void processAnnotation() {
        getBasicField();
        generateConstructorMethod();
        generateLayoutIdMethod();
        generateBindMethod();
        try {
            generateAdapter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取TimeLine标注的类的基本信息
     */
    private void getBasicField() {
        if (this.element == null) {
            throw new RuntimeException("element is null");
        }
        packageName = TimeLineContext.elementUtil.getPackageOf(element).getQualifiedName().toString();
        className = element.getSimpleName().toString();
        fullClassName = packageName + "." + className;
        adapterName = AdapterUtil.generateAdapterName(element, className);
        noteBasicInfo();
    }

    private void generateConstructorMethod() {
        constructorMethod = AdapterUtil.constructorMethod(fullClassName);
        TimeLineContext.note(constructorMethod.toString());
    }

    /**
     * 获取布局文件的id，并生成对应的复写方法
     */
    private void generateLayoutIdMethod() {
        keyLayoutId = element.getAnnotation(TimeLine.class).keyLayoutId();
        getKeyLayoutIdMethod = AdapterUtil.generateOverRideIdMethod(keyLayoutId,
                "getKeyLayoutId");
        valueLayoutId = element.getAnnotation(TimeLine.class).valueLayoutId();
        getValueLayoutIdMethod = AdapterUtil.generateOverRideIdMethod(valueLayoutId,
                "getValueLayoutId");
        noteLayoutIdMethod();
    }

    private void generateBindMethod() {
        List<TextViewAttr> keyTextView = new ArrayList<>();
        List<ImageViewAttr> keyImageView = new ArrayList<>();
        List<TextViewAttr> valueTextView = new ArrayList<>();
        List<ImageViewAttr> valueImageView = new ArrayList<>();
        AdapterUtil.getTextViewAttr(keyTextView, valueTextView, element);

        AdapterUtil.getImageViewAttr(keyImageView, valueImageView, element);

        bindKeyItemMethod = AdapterUtil.generateBindMethod("bindKeyItem",
                fullClassName, keyTextView, keyImageView, "KeyViewHolder");

        TimeLineContext.note("----bindKeyItemMethod");
        bindValueItemMethod = AdapterUtil.generateBindMethod("bindValueItem",
                fullClassName, valueTextView, valueImageView, "ValueViewHolder");
    }

    private void generateAdapter() throws IOException {
        TypeName superClass = ParameterizedTypeName
                .get(ClassName.bestGuess("io.github.rangaofei.sakatimeline.adapter.AbstractTimeLineAdapter"), ClassName.bestGuess(element.getSimpleName().toString()));
        TypeSpec adapter = TypeSpec.classBuilder(adapterName)
                .superclass(superClass)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(Arrays.asList(constructorMethod, getKeyLayoutIdMethod, getValueLayoutIdMethod,
                        bindKeyItemMethod, bindValueItemMethod))
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, adapter)
                .build();
        javaFile.writeTo(TimeLineContext.filter);
    }

    /**
     * 日志输出标注的类的基本信息
     */
    private void noteBasicInfo() {
        TimeLineContext.note(">>>>packageName=%s", packageName);
        TimeLineContext.note(">>>>className=%s", className);
        TimeLineContext.note(">>>>fullClassName=%s", fullClassName);
        TimeLineContext.note(">>>>adapterName=%s", adapterName);
    }

    private void noteLayoutIdMethod() {
        TimeLineContext.note(">>>>keyLayoutId:%s", getKeyLayoutIdMethod.toString());
        TimeLineContext.note(">>>>valueLayoutId:%s", getValueLayoutIdMethod.toString());
    }
}
