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

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;

import org.springframework.boot.actuate.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry logging exports.
 *
 * @author Toshiaki Maki
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ConditionalOnEnabledLoggingExport.class, OpenTelemetry.class, SdkLoggerProvider.class })
@ConditionalOnEnabledLoggingExport("opentelemetry")
@EnableConfigurationProperties(OpenTelemetryLoggingExportProperties.class)
@Import({ OpenTelemetryLoggingConnectionDetailsConfiguration.class, OpenTelemetryLoggingTransportConfiguration.class })
public final class OpenTelemetryLoggingExportAutoConfiguration {

}
