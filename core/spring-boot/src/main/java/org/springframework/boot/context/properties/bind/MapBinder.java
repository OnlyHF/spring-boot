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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;

/**
 * {@link AggregateBinder} for Maps.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapBinder extends AggregateBinder<Map<Object, Object>> {

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

	MapBinder(Context context) {
		super(context);
	}

	@Override
	protected boolean isAllowRecursiveBinding(@Nullable ConfigurationPropertySource source) {
		return true;
	}

	@Override
	protected @Nullable Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Bindable<?> resolvedTarget = resolveTarget(target);
		boolean hasDescendants = hasDescendants(name);
		if (!hasDescendants && !ConfigurationPropertyName.EMPTY.equals(name)) {
			for (ConfigurationPropertySource source : getContext().getSources()) {
				ConfigurationProperty property = source.getConfigurationProperty(name);
				if (property != null) {
					getContext().setConfigurationProperty(property);
					Object result = getContext().getPlaceholdersResolver().resolvePlaceholders(property.getValue());
					return getContext().getConverter().convert(result, target);
				}
			}
		}
		Map<Object, Object> map = createMap(target);
		for (ConfigurationPropertySource source : getContext().getSources()) {
			if (!ConfigurationPropertyName.EMPTY.equals(name)) {
				source = source.filter(name::isAncestorOf);
			}
			new EntryBinder(name, resolvedTarget, elementBinder).bindEntries(source, map);
		}
		return map.isEmpty() ? null : map;
	}

	private Map<Object, Object> createMap(Bindable<?> target) {
		Class<?> mapType = (target.getValue() != null) ? Map.class : target.getType().resolve(Object.class);
		if (EnumMap.class.isAssignableFrom(mapType)) {
			Class<?> keyType = target.getType().asMap().resolveGeneric(0);
			return CollectionFactory.createMap(mapType, keyType, 0);
		}
		return CollectionFactory.createMap(mapType, 0);
	}

	private boolean hasDescendants(ConfigurationPropertyName name) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	private Bindable<?> resolveTarget(Bindable<?> target) {
		Class<?> type = target.getType().resolve(Object.class);
		if (Properties.class.isAssignableFrom(type)) {
			return STRING_STRING_MAP;
		}
		return target;
	}

	@Override
	protected Map<Object, Object> merge(Supplier<Map<Object, Object>> existing, Map<Object, Object> additional) {
		Map<Object, Object> existingMap = getExistingIfPossible(existing);
		if (existingMap == null) {
			return additional;
		}
		try {
			existingMap.putAll(additional);
			return copyIfPossible(existingMap);
		}
		catch (UnsupportedOperationException ex) {
			Map<Object, Object> result = createNewMap(additional.getClass(), existingMap);
			result.putAll(additional);
			return result;
		}
	}

	private @Nullable Map<Object, Object> getExistingIfPossible(Supplier<Map<Object, Object>> existing) {
		try {
			return existing.get();
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Map<Object, Object> copyIfPossible(Map<Object, Object> map) {
		try {
			return createNewMap(map.getClass(), map);
		}
		catch (Exception ex) {
			return map;
		}
	}

	private Map<Object, Object> createNewMap(Class<?> mapClass, Map<Object, Object> map) {
		Map<Object, Object> result = CollectionFactory.createMap(mapClass, map.size());
		result.putAll(map);
		return result;
	}

	private class EntryBinder {

		private final ConfigurationPropertyName root;

		private final AggregateElementBinder elementBinder;

		private final ResolvableType mapType;

		private final ResolvableType keyType;

		private final ResolvableType valueType;

		private final Class<?> resolvedValueType;

		private final boolean valueTreatedAsNestedMap;

		private final Bindable<Object> bindableMapType;

		private final Bindable<Object> bindableValueType;

		EntryBinder(ConfigurationPropertyName root, Bindable<?> target, AggregateElementBinder elementBinder) {
			this.root = root;
			this.elementBinder = elementBinder;
			this.mapType = target.getType().asMap();
			this.keyType = this.mapType.getGeneric(0);
			this.valueType = this.mapType.getGeneric(1);
			this.resolvedValueType = this.valueType.resolve(Object.class);
			this.valueTreatedAsNestedMap = Object.class.equals(this.resolvedValueType);
			this.bindableMapType = Bindable.of(this.mapType);
			this.bindableValueType = Bindable.of(this.valueType);
		}

		void bindEntries(ConfigurationPropertySource source, Map<Object, Object> map) {
			if (source instanceof IterableConfigurationPropertySource iterableSource) {
				for (ConfigurationPropertyName name : iterableSource) {
					ConfigurationPropertyName entryName = getEntryName(source, name);
					Object key = getContext().getConverter().convert(getKeyName(entryName), this.keyType);
					Bindable<?> valueBindable = getValueBindable(name);
					map.computeIfAbsent(key, (k) -> this.elementBinder.bind(entryName, valueBindable));
				}
			}
		}

		private Bindable<?> getValueBindable(ConfigurationPropertyName name) {
			return (!isParentOf(name) && this.valueTreatedAsNestedMap) ? this.bindableMapType : this.bindableValueType;
		}

		private ConfigurationPropertyName getEntryName(ConfigurationPropertySource source,
				ConfigurationPropertyName name) {
			if (Collection.class.isAssignableFrom(this.resolvedValueType) || this.valueType.isArray()) {
				return chopNameAtNumericIndex(name);
			}
			if (!isParentOf(name) && (this.valueTreatedAsNestedMap || !isScalarValue(source, name))) {
				return name.chop(this.root.getNumberOfElements() + 1);
			}
			return name;
		}

		private boolean isParentOf(ConfigurationPropertyName name) {
			return this.root.isParentOf(name);
		}

		private ConfigurationPropertyName chopNameAtNumericIndex(ConfigurationPropertyName name) {
			int start = this.root.getNumberOfElements() + 1;
			int size = name.getNumberOfElements();
			for (int i = start; i < size; i++) {
				if (name.isNumericIndex(i)) {
					return name.chop(i);
				}
			}
			return name;
		}

		private boolean isScalarValue(ConfigurationPropertySource source, ConfigurationPropertyName name) {
			Class<?> resolved = this.valueType.resolve(Object.class);
			if (!resolved.getName().startsWith("java.lang") && !resolved.isEnum()) {
				return false;
			}
			ConfigurationProperty property = source.getConfigurationProperty(name);
			if (property == null) {
				return false;
			}
			Object value = property.getValue();
			value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
			return getContext().getConverter().canConvert(value, this.valueType);
		}

		private String getKeyName(ConfigurationPropertyName name) {
			StringBuilder result = new StringBuilder();
			for (int i = this.root.getNumberOfElements(); i < name.getNumberOfElements(); i++) {
				if (!result.isEmpty()) {
					result.append('.');
				}
				result.append(name.getElement(i, Form.ORIGINAL));
			}
			return result.toString();
		}

	}

}
