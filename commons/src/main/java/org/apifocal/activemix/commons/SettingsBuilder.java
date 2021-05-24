/*
 * Copyright (c) 2017-2021 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.activemix.commons;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Settings utility for reading LoginModule configuration properties in more handy way.
 */
public final class SettingsBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(SettingsBuilder.class);


    public static <T> List<T> createObjects(
    		final ClassLoader cl, final Settings settings, 
    		String packageName, String typeNames, Class<T> baseType, String configPrefix) {
        return Arrays.stream(typeNames.split(","))
            .map(String::trim)
            .map(clazz -> load(packageName, clazz, baseType, cl))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(validatorClass -> create(validatorClass, settings.subset(configPrefix, validatorClass.getSimpleName())))
            .collect(Collectors.toList());
    }

    public static <T> T create(Class<T> typeClass, Settings settings) {
    	if (typeClass == null) {
            return null;
    	}
        try {
            Constructor<T> constructor = typeClass.getConstructor(Settings.class);
            return constructor.newInstance(settings);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find a constructor with Settings argument in type " + typeClass.getName(), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Could not instantiate type " + typeClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access a constructor with Settings argument in type " + typeClass.getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not initialize instance of type " + typeClass.getName(), e.getTargetException());
        }
    }

    public static <T> Optional<Class<T>> load(String pkg, String clazz, Class<T> baseType, ClassLoader classLoader) {
        if (clazz.contains(".")) {
            return loadClass(classLoader, baseType, clazz);
        }

        return Optional.of(pkg)
            .map(String::trim)
            .filter(prefix -> !prefix.isEmpty() && !clazz.contains("."))
            .map(prefix -> loadClass(classLoader, baseType, prefix + "." + clazz))
            .orElseGet(() -> loadClass(classLoader, baseType, clazz));
    }

    public static <T> Optional<Class<T>> loadClass(ClassLoader classLoader, Class<T> baseType, String clazz) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new CompositeClassLoader(contextClassLoader, classLoader));
            Class<?> aClass = classLoader.loadClass(clazz);
            if (baseType.isAssignableFrom(aClass)) {
                return Optional.of(aClass.asSubclass(baseType));
            }
        } catch (ClassNotFoundException e) {
            LOG.info("Failed to find type {}", clazz);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return Optional.empty();
    }

    public static Supplier<? extends RuntimeException> requiredPropertyMissing(String optionName) {
        return () -> new IllegalStateException("Option " + optionName + " must be specified");
    }

    public static class CompositeClassLoader extends ClassLoader {
        private final Logger logger = LoggerFactory.getLogger(CompositeClassLoader.class);
        private final ClassLoader[] classLoaders;

        CompositeClassLoader(ClassLoader ... classLoaders) {
            this.classLoaders = classLoaders;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            for (ClassLoader loader : classLoaders) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    logger.debug("Couldn't find class {} in class laoder {}", name, loader);
                }
            }
            return super.loadClass(name);
        }
    }

}
