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

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Internal strategy used by {@link Binder} to bind aggregates (Maps, Lists, Arrays).
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class AggregateBinder<T> {

	private final Context context;

	AggregateBinder(Context context) {
		this.context = context;
	}

	/**
	 * Determine if recursive binding is supported.
	 * @param source the configuration property source or {@code null} for all sources.
	 * @return if recursive binding is supported
	 */
	protected abstract boolean isAllowRecursiveBinding(@Nullable ConfigurationPropertySource source);

	/**
	 * Perform binding for the aggregate.
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound aggregate or null
	 */
	@SuppressWarnings("unchecked")
	final @Nullable Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Object result = bindAggregate(name, target, elementBinder);
		Supplier<?> value = target.getValue();
		if (result == null || value == null) {
			return result;
		}
		return merge((Supplier<T>) value, (T) result);
	}

	/**
	 * Perform the actual aggregate binding.
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound result
	 */
	protected abstract @Nullable Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder);

	/**
	 * Merge any additional elements into the existing aggregate.
	 * @param existing the supplier for the existing value
	 * @param additional the additional elements to merge
	 * @return the merged result
	 */
	protected abstract T merge(Supplier<T> existing, T additional);

	/**
	 * Return the context being used by this binder.
	 * @return the context
	 */
	protected final Context getContext() {
		return this.context;
	}

	/**
	 * Internal class used to supply the aggregate and cache the value.
	 *
	 * @param <T> the aggregate type
	 */
	protected static class AggregateSupplier<T> {

		private final Supplier<T> supplier;

		private @Nullable T supplied;

		public AggregateSupplier(Supplier<T> supplier) {
			this.supplier = supplier;
		}

		public T get() {
			if (this.supplied == null) {
				this.supplied = this.supplier.get();
			}
			return this.supplied;
		}

		public boolean wasSupplied() {
			return this.supplied != null;
		}

	}

}
