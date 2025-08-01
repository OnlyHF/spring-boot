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

package org.springframework.boot.graphql.autoconfigure.rsocket;

import graphql.GraphQL;
import io.rsocket.RSocket;
import io.rsocket.transport.netty.client.TcpClientTransport;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.graphql.client.RSocketGraphQlClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RSocketGraphQlClient}.
 * This auto-configuration creates
 * {@link org.springframework.graphql.client.RSocketGraphQlClient.Builder
 * RSocketGraphQlClient.Builder} prototype beans, as the builders are stateful and should
 * not be reused to build client instances with different configurations.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.rsocket.autoconfigure.RSocketRequesterAutoConfiguration")
@ConditionalOnClass({ GraphQL.class, RSocketGraphQlClient.class, RSocketRequester.class, RSocket.class,
		TcpClientTransport.class })
public final class RSocketGraphQlClientAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	RSocketGraphQlClient.Builder<?> rsocketGraphQlClientBuilder(RSocketRequester.Builder rsocketRequesterBuilder) {
		return RSocketGraphQlClient.builder(rsocketRequesterBuilder.dataMimeType(MimeTypeUtils.APPLICATION_JSON));
	}

}
