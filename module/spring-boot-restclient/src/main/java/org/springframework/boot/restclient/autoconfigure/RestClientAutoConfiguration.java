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

package org.springframework.boot.restclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClient}.
 * <p>
 * This will produce a {@link Builder RestClient.Builder} bean with the {@code prototype}
 * scope, meaning each injection point will receive a newly cloned instance of the
 * builder.
 *
 * @author Arjen Poutsma
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { HttpClientAutoConfiguration.class, TaskExecutionAutoConfiguration.class, SslAutoConfiguration.class })
@ConditionalOnClass(RestClient.class)
@Conditional(NotReactiveWebApplicationOrVirtualThreadsExecutorEnabledCondition.class)
public final class RestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(RestClientSsl.class)
	@ConditionalOnBean(SslBundles.class)
	AutoConfiguredRestClientSsl restClientSsl(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings, SslBundles sslBundles) {
		return new AutoConfiguredRestClientSsl(
				clientHttpRequestFactoryBuilder.getIfAvailable(ClientHttpRequestFactoryBuilder::detect),
				clientHttpRequestFactorySettings.getIfAvailable(ClientHttpRequestFactorySettings::defaults),
				sslBundles);
	}

	@Bean
	@ConditionalOnMissingBean
	RestClientBuilderConfigurer restClientBuilderConfigurer(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings,
			ObjectProvider<RestClientCustomizer> customizerProvider) {
		return new RestClientBuilderConfigurer(
				clientHttpRequestFactoryBuilder.getIfAvailable(ClientHttpRequestFactoryBuilder::detect),
				clientHttpRequestFactorySettings.getIfAvailable(ClientHttpRequestFactorySettings::defaults),
				customizerProvider.orderedStream().toList());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
		return restClientBuilderConfigurer.configure(RestClient.builder());
	}

	@ConditionalOnClass(HttpMessageConverters.class)
	static class HttpMessageConvertersConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Order(Ordered.LOWEST_PRECEDENCE)
		HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
				ObjectProvider<HttpMessageConverters> messageConverters) {
			return new HttpMessageConvertersRestClientCustomizer(messageConverters.getIfUnique());
		}

	}

}
