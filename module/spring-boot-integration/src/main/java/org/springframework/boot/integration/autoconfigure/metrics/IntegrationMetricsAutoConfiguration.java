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

package org.springframework.boot.integration.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Integration's metrics.
 * Orders auto-configuration classes to ensure that the {@link MeterRegistry} bean has
 * been defined before Spring Integration's Micrometer support queries the bean factory
 * for it.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = IntegrationAutoConfiguration.class,
		afterName = "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
public final class IntegrationMetricsAutoConfiguration {

}
