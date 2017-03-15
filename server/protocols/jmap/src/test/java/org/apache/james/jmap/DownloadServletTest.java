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

package org.apache.james.jmap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.api.SimpleTokenFactory;
import org.apache.james.jmap.utils.DownloadPath;
import org.apache.james.mailbox.AttachmentManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.junit.Test;

public class DownloadServletTest {

    @Test
    public void downloadMayFailWhenUnknownErrorOnAttachmentManager() throws Exception {
        MailboxSession mailboxSession = mock(MailboxSession.class);
        AttachmentManager mockedAttachmentManager = mock(AttachmentManager.class);
        when(mockedAttachmentManager.getAttachment(any(), eq(mailboxSession)))
            .thenThrow(new MailboxException());
        SimpleTokenFactory nullSimpleTokenFactory = null;

        DownloadServlet testee = new DownloadServlet(mockedAttachmentManager, nullSimpleTokenFactory, new NoopMetricFactory());

        HttpServletResponse resp = mock(HttpServletResponse.class);
        testee.download(mailboxSession, DownloadPath.from("/blobId"), resp);

        verify(resp).setStatus(500);
    }
}