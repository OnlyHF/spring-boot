/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

	@Override
	public @Nullable Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		Constructors constructors = Constructors.getConstructors(bindable.getType().resolve(),
				isNestedConstructorBinding);
		if (constructors.getBind() != null && constructors.isDeducedBindConstructor()
				&& !constructors.isImmutableType()) {
			if (bindable.getValue() != null && bindable.getValue().get() != null) {
				return null;
			}
		}
		return constructors.getBind();
	}

	@Override
	public @Nullable Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
		Constructors constructors = Constructors.getConstructors(type, isNestedConstructorBinding);
		return constructors.getBind();
	}

	/**
	 * Data holder for autowired and bind constructors.
	 */
	static final class Constructors {

		private static final Constructors NONE = new Constructors(false, null, false, false);

		private final boolean hasAutowired;

		private final @Nullable Constructor<?> bind;

		private final boolean deducedBindConstructor;

		private final boolean immutableType;

		private Constructors(boolean hasAutowired, @Nullable Constructor<?> bind, boolean deducedBindConstructor,
				boolean immutableType) {
			this.hasAutowired = hasAutowired;
			this.bind = bind;
			this.deducedBindConstructor = deducedBindConstructor;
			this.immutableType = immutableType;
		}

		boolean hasAutowired() {
			return this.hasAutowired;
		}

		@Nullable Constructor<?> getBind() {
			return this.bind;
		}

		boolean isDeducedBindConstructor() {
			return this.deducedBindConstructor;
		}

		boolean isImmutableType() {
			return this.immutableType;
		}

		static Constructors getConstructors(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
			if (type == null) {
				return NONE;
			}
			boolean hasAutowiredConstructor = isAutowiredPresent(type);
			Constructor<?>[] candidates = getCandidateConstructors(type);
			MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
			boolean deducedBindConstructor = false;
			boolean immutableType = type.isRecord();
			Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
			if (bind == null && !hasAutowiredConstructor) {
				bind = deduceBindConstructor(type, candidates);
				deducedBindConstructor = bind != null;
			}
			if (bind == null && !hasAutowiredConstructor && isKotlinType(type)) {
				bind = deduceKotlinBindConstructor(type);
				deducedBindConstructor = bind != null;
			}
			if (bind != null || isNestedConstructorBinding) {
				Assert.state(!hasAutowiredConstructor,
						() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
			}
			return new Constructors(hasAutowiredConstructor, bind, deducedBindConstructor, immutableType);
		}

		private static boolean isAutowiredPresent(Class<?> type) {
			if (Stream.of(type.getDeclaredConstructors())
				.map(MergedAnnotations::from)
				.anyMatch((annotations) -> annotations.isPresent(Autowired.class))) {
				return true;
			}
			Class<?> userClass = ClassUtils.getUserClass(type);
			return (userClass != type) && isAutowiredPresent(userClass);
		}

		private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
			if (isInnerClass(type)) {
				return new Constructor<?>[0];
			}
			return Arrays.stream(type.getDeclaredConstructors())
				.filter(Constructors::isNonSynthetic)
				.toArray(Constructor[]::new);
		}

		private static boolean isInnerClass(Class<?> type) {
			try {
				return type.getDeclaredField("this$0").isSynthetic();
			}
			catch (NoSuchFieldException ex) {
				return false;
			}
		}

		private static boolean isNonSynthetic(Constructor<?> constructor) {
			return !constructor.isSynthetic();
		}

		private static MergedAnnotations[] getAnnotations(Constructor<?>[] candidates) {
			MergedAnnotations[] candidateAnnotations = new MergedAnnotations[candidates.length];
			for (int i = 0; i < candidates.length; i++) {
				candidateAnnotations[i] = MergedAnnotations.from(candidates[i], SearchStrategy.SUPERCLASS);
			}
			return candidateAnnotations;
		}

		private static @Nullable Constructor<?> getConstructorBindingAnnotated(Class<?> type,
				Constructor<?>[] candidates, MergedAnnotations[] mergedAnnotations) {
			Constructor<?> result = null;
			for (int i = 0; i < candidates.length; i++) {
				if (mergedAnnotations[i].isPresent(ConstructorBinding.class)) {
					Assert.state(candidates[i].getParameterCount() > 0,
							() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
					Assert.state(result == null,
							() -> type.getName() + " has more than one @ConstructorBinding constructor");
					result = candidates[i];
				}
			}
			return result;

		}

		private static @Nullable Constructor<?> deduceBindConstructor(Class<?> type, Constructor<?>[] candidates) {
			if (candidates.length == 1 && candidates[0].getParameterCount() > 0) {
				if (type.isMemberClass() && Modifier.isPrivate(candidates[0].getModifiers())) {
					return null;
				}
				return candidates[0];
			}
			Constructor<?> result = null;
			for (Constructor<?> candidate : candidates) {
				if (!Modifier.isPrivate(candidate.getModifiers())) {
					if (result != null) {
						return null;
					}
					result = candidate;
				}
			}
			return (result != null && result.getParameterCount() > 0) ? result : null;
		}

		private static boolean isKotlinType(Class<?> type) {
			return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
		}

		private static @Nullable Constructor<?> deduceKotlinBindConstructor(Class<?> type) {
			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
				return primaryConstructor;
			}
			return null;
		}

	}

}
