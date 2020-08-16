package com.adelean.inject.resources.spring.core;

import com.adelean.inject.resources.spring.BinaryResource;
import com.adelean.inject.resources.spring.TextResource;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.leangen.geantyref.TypeFactory.annotation;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.reflections.ReflectionUtils.getAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class AnnotationsTests {

    @Test
    @DisplayName("Test find single resource annotation")
    public void testFindSingleResourceAnnotation() throws Exception {

        /* Given */
        var binaryField = TestClass.class.getDeclaredField("binaryField");
        var notResourceField = TestClass.class.getDeclaredField("notResourceField");
        var fieldWithTwoResourceAnnotations = TestClass.class.getDeclaredField("fieldWithTwoResourceAnnotations");
        var parameterWithTwoResourceAnnotations = TestClass.class
                .getDeclaredConstructor(Object.class)
                .getParameters()[0];
        var setTwoResources = TestClass.class.getDeclaredMethod("setTwoResources", Object.class);

        /* When */
        var binaryResourceAnnotation = Annotations.findSingleResourceAnnotation(binaryField);
        var noResourceAnnotation = Annotations.findSingleResourceAnnotation(notResourceField);

        ThrowableAssert.ThrowingCallable findFromFieldWithTwoAnnotations = () -> Annotations
                .findSingleResourceAnnotation(fieldWithTwoResourceAnnotations);
        ThrowableAssert.ThrowingCallable findFromParameterWithTwoAnnotations = () -> Annotations
                .findSingleResourceAnnotation(parameterWithTwoResourceAnnotations);
        ThrowableAssert.ThrowingCallable findFromMethodWithTwoAnnotations = () -> Annotations
                .findSingleResourceAnnotation(setTwoResources);

        /* Then */
        assertThat(binaryResourceAnnotation)
                .isNotNull()
                .isInstanceOf(BinaryResource.class)
                .hasFieldOrPropertyWithValue("value", "/com/adelean/junit/jupiter/fibonacci.bin");
        assertThat(noResourceAnnotation)
                .isNull();

        assertThatCode(findFromFieldWithTwoAnnotations)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Field 'fieldWithTwoResourceAnnotations' has multiple resource annotations.");
        assertThatCode(findFromParameterWithTwoAnnotations)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Parameter 'arg0' has multiple resource annotations.");
        assertThatCode(findFromMethodWithTwoAnnotations)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Method 'setTwoResources' has multiple resource annotations.");
    }

    @Test
    @DisplayName("Test assert no other annotations")
    public void testAssertNoOtherAnnotations() throws Exception {

        /* Given */
        var validField = AssertsTests.TestClass.class.getDeclaredField("validField");
        var invalidField = AssertsTests.TestClass.class.getDeclaredField("invalidField");
        var invalidParameter = AssertsTests.TestClass.class.getDeclaredConstructor(String.class).getParameters()[0];
        var invalidSetter = AssertsTests.TestClass.class.getDeclaredMethod("invalidSetter", String.class);

        /* When */
        ThrowableAssert.ThrowingCallable assertValidField = () -> Annotations.assertNoOtherAnnotations(
                validField, resourceAnnotation(validField));
        ThrowableAssert.ThrowingCallable assertInvalidField = () -> Annotations.assertNoOtherAnnotations(
                invalidField, resourceAnnotation(invalidField));
        ThrowableAssert.ThrowingCallable assertInvalidParameter = () -> Annotations.assertNoOtherAnnotations(
                invalidParameter, resourceAnnotation(invalidParameter));
        ThrowableAssert.ThrowingCallable assertInvalidSetter = () -> Annotations.assertNoOtherAnnotations(
                invalidSetter, resourceAnnotation(invalidSetter));

        /* Then */
        assertThatCode(assertValidField)
                .doesNotThrowAnyException();
        assertThatCode(assertInvalidField)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Field 'invalidField' annotated with @TextResource has other invalid annotations:\n"
                        + "\t- @Autowired");
        assertThatCode(assertInvalidParameter)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Parameter 'arg0' annotated with @TextResource has other invalid annotations:\n"
                        + "\t- @Autowired");
        assertThatCode(assertInvalidSetter)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Method 'invalidSetter' annotated with @TextResource has other invalid annotations:\n"
                        + "\t- @Autowired");
    }

    @Test
    @DisplayName("Test get invalid annotations")
    public void testInvalidAnnotations() throws Exception {

        /* Given */
        var autowiredResourceField = TestClass.class.getDeclaredField("autowiredResourceField");
        var resourceAnnotation = autowiredResourceField.getDeclaredAnnotation(TextResource.class);

        /* When */
        var invalidAnnotations = Annotations.invalidAnnotations(autowiredResourceField, resourceAnnotation);

        /* Then */
        assertThat(invalidAnnotations)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1)
                .contains(annotation(Autowired.class, emptyMap()));
    }

    @Test
    @DisplayName("Test annotations to string")
    public void testAnnotationsToString() throws Exception {

        /* Given */
        Annotation[] annotations = {
                annotation(Autowired.class, emptyMap()),
                annotation(BinaryResource.class, singletonMap("value", "path1")),
                annotation(TextResource.class, singletonMap("value", "path2"))
        };

        /* When */
        var annotationsToString = Annotations.annotationsToString(annotations);

        /* Then */
        assertThat(annotationsToString)
                .isNotNull()
                .isNotEmpty()
                .isNotBlank()
                .isEqualTo("\t- @Autowired\n\t- @BinaryResource\n\t- @TextResource");
    }

    @SuppressWarnings("unchecked")
    static Annotation resourceAnnotation(AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement)
                .stream()
                .filter(annotation -> annotation instanceof TextResource)
                .findAny()
                .orElse(null);
    }

    static class TestClass {

        @BinaryResource("/com/adelean/junit/jupiter/fibonacci.bin")
        byte[] binaryField;

        String notResourceField;

        @BinaryResource("/com/adelean/junit/jupiter/fibonacci.bin")
        @TextResource("/com/adelean/junit/jupiter/resource.txt")
        Object fieldWithTwoResourceAnnotations;

        @Autowired
        @TextResource("/com/adelean/junit/jupiter/resource.txt")
        Object autowiredResourceField;

        TestClass(
                @BinaryResource("/com/adelean/junit/jupiter/fibonacci.bin")
                @TextResource("/com/adelean/junit/jupiter/resource.txt")
                Object parameterWithTwoResourceAnnotations) {
        }

        @BinaryResource("/com/adelean/junit/jupiter/fibonacci.bin")
        @TextResource("/com/adelean/junit/jupiter/resource.txt")
        void setTwoResources(Object ignored) {
        }
    }
}
