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

package org.springframework.boot.artemis.autoconfigure;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.jms.autoconfigure.JmsProperties;
import org.springframework.boot.jms.autoconfigure.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to integrate with an Artemis broker.
 * If the necessary classes are present, embed the broker in the application by default.
 * Otherwise, connect to a broker available on the local machine with the default
 * settings.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see ArtemisProperties
 */
@AutoConfiguration(before = JmsAutoConfiguration.class,
		after = { JndiConnectionFactoryAutoConfiguration.class, JtaAutoConfiguration.class })
@ConditionalOnClass({ ConnectionFactory.class, ActiveMQConnectionFactory.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({ ArtemisProperties.class, JmsProperties.class })
@Import({ ArtemisEmbeddedServerConfiguration.class, ArtemisXAConnectionFactoryConfiguration.class,
		ArtemisConnectionFactoryConfiguration.class })
public final class ArtemisAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ArtemisConnectionDetails artemisConnectionDetails(ArtemisProperties properties) {
		return new PropertiesArtemisConnectionDetails(properties);
	}

	/**
	 * Adapts {@link ArtemisProperties} to {@link ArtemisConnectionDetails}.
	 */
	static class PropertiesArtemisConnectionDetails implements ArtemisConnectionDetails {

		private final ArtemisProperties properties;

		PropertiesArtemisConnectionDetails(ArtemisProperties properties) {
			this.properties = properties;
		}

		@Override
		public ArtemisMode getMode() {
			return this.properties.getMode();
		}

		@Override
		public String getBrokerUrl() {
			return this.properties.getBrokerUrl();
		}

		@Override
		public String getUser() {
			return this.properties.getUser();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

	}

}
