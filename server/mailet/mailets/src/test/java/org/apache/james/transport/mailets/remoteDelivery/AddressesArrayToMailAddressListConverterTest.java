/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets.remoteDelivery;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.apache.mailet.base.MailAddressFixture;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressesArrayToMailAddressListConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressesArrayToMailAddressListConverterTest.class);

    @Test
    public void getAddressesAsMailAddressShouldReturnEmptyOnNull() {
        assertThat(AddressesArrayToMailAddressListConverter.getAddressesAsMailAddress(null, LOGGER)).isEmpty();
    }

    @Test
    public void getAddressesAsMailAddressShouldReturnEmptyOnEmpty() {
        assertThat(AddressesArrayToMailAddressListConverter.getAddressesAsMailAddress(new Address[]{}, LOGGER)).isEmpty();
    }

    @Test
    public void getAddressesAsMailAddressShouldWorkWithSingleValue() throws Exception {
        assertThat(AddressesArrayToMailAddressListConverter.getAddressesAsMailAddress(new Address[]{
            new InternetAddress(MailAddressFixture.ANY_AT_JAMES.toString())}, LOGGER))
            .containsOnly(MailAddressFixture.ANY_AT_JAMES);
    }

    @Test
    public void getAddressesAsMailAddressShouldWorkWithTwoValues() throws Exception {
        assertThat(AddressesArrayToMailAddressListConverter.getAddressesAsMailAddress(new Address[]{
            new InternetAddress(MailAddressFixture.ANY_AT_JAMES.toString()),
            new InternetAddress(MailAddressFixture.OTHER_AT_JAMES.toString())}, LOGGER))
            .containsOnly(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES);
    }
}
