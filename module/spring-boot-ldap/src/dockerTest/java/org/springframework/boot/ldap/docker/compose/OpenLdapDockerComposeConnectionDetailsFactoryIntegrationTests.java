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

package org.springframework.boot.ldap.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenLdapDockerComposeConnectionDetailsFactory}.
 *
 * @author Philipp Kessler
 */
class OpenLdapDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "ldap-compose.yaml", image = TestImage.OPEN_LDAP)
	void runCreatesConnectionDetails(LdapConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("cn=admin,dc=ldap,dc=example,dc=org");
		assertThat(connectionDetails.getPassword()).isEqualTo("somepassword");
		assertThat(connectionDetails.getBase()).isEqualTo("dc=ldap,dc=example,dc=org");
		assertThat(connectionDetails.getUrls()).hasSize(1);
		assertThat(connectionDetails.getUrls()[0]).startsWith("ldaps://");
	}

}
