package com.adelean.junit.jupiter.resources.json;

import com.adelean.junit.jupiter.resources.core.AbstractResourcesInjector;
import com.adelean.junit.jupiter.resources.core.ResourceParser;
import com.adelean.junit.jupiter.resources.core.cdi.InjectionContext;
import com.adelean.junit.jupiter.resources.core.helpers.ClasspathSupport;
import com.adelean.junit.jupiter.resources.dsl.ResourceAsReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Optional;

abstract class AbstractJacksonResourcesInjector<A extends Annotation> extends AbstractResourcesInjector<A> {
    private static final String ERR_NAMED_JACKSON_MAPPER_MISSING =
            "Can't find named ObjectMapper '%s' in execution context. You probably forgot @WithJacksonMapper(\"%s\").";
    private static final String ERR_NAMED_GSON_MISSING =
            "Can't find named Gson '%s' in execution context. You probably forgot @WithGson(\"%s\").";
    private static final String ERR_BOTH_JACKSON_AND_GSON_ON_ANNOTATION =
            "@GivenJsonResource cannot have both 'jacksonMapper' & 'gson'.";
    private static final String ERR_JACKSON_AND_GSON_MISSING_ON_CLASS_PATH =
            "Jackson & Gson are missing on classpath.";

    protected AbstractJacksonResourcesInjector(
            InjectionContext injectionContext,
            @Nullable
            Object testInstance,
            Class<?> testClass,
            Class<A> annotationType) {
        super(injectionContext, testInstance, testClass, annotationType);
    }

    protected JsonParser findJsonParser(A resourceAnnotation) {
        boolean parsedWithJackson = isParsedWithJackson(resourceAnnotation);
        boolean parsedWithGson = isParsedWithGson(resourceAnnotation);

        if (parsedWithJackson && !parsedWithGson) {
            return getNamedJacksonParser(jacksonMapperName(resourceAnnotation));
        } else if (!parsedWithJackson && parsedWithGson) {
            return getNamedGsonParser(gsonName(resourceAnnotation));
        } else if (!parsedWithJackson) {
            if (ClasspathSupport.isJackson2Present()) {
                return findAnonymousJacksonParser()
                        .orElseGet(() -> new JacksonResourceParser(new ObjectMapper()));
            } else if (ClasspathSupport.isGsonPresent()) {
                return findAnonymousGsonParser()
                        .orElseGet(() -> new GsonResourceParser(new Gson()));
            } else {
                throw new RuntimeException(ERR_JACKSON_AND_GSON_MISSING_ON_CLASS_PATH);
            }
        } else {
            throw new ExtensionConfigurationException(ERR_BOTH_JACKSON_AND_GSON_ON_ANNOTATION);
        }
    }

    protected JacksonResourceParser getNamedJacksonParser(String jacksonMapperName) {
        if (StringUtils.isBlank(jacksonMapperName)) {
            throw new IllegalArgumentException("Argument 'jacksonMapperName' must not be a blank string");
        }

        return injectionContext
                .findBean(testClass, jacksonMapperName, JacksonResourceParser.class)
                .orElseThrow(() -> new RuntimeException(String.format(
                        ERR_NAMED_JACKSON_MAPPER_MISSING, jacksonMapperName, jacksonMapperName)));
    }

    protected GsonResourceParser getNamedGsonParser(String gsonName) {
        if (StringUtils.isBlank(gsonName)) {
            throw new IllegalArgumentException("Argument 'gsonName' must not be a blank string");
        }

        return injectionContext
                .findBean(testClass, gsonName, GsonResourceParser.class)
                .orElseThrow(() -> new RuntimeException(String.format(
                        ERR_NAMED_GSON_MISSING, gsonName, gsonName)));
    }

    protected Optional<JacksonResourceParser> findAnonymousJacksonParser() {
        return injectionContext.findBean(testClass, null, JacksonResourceParser.class);
    }

    protected Optional<GsonResourceParser> findAnonymousGsonParser() {
        return injectionContext.findBean(testClass, null, GsonResourceParser.class);
    }

    abstract boolean isParsedWithJackson(A resourceAnnotation);
    abstract String jacksonMapperName(A resourceAnnotation);
    abstract boolean isParsedWithGson(A resourceAnnotation);
    abstract String gsonName(A resourceAnnotation);
}
