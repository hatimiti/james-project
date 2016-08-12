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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.mailbox.exception.MailboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class UploadServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadServlet.class);

    private final UploadHandler uploadHandler;

    @Inject
    private UploadServlet(UploadHandler uploadHandler) {
        this.uploadHandler = uploadHandler;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        String contentType = req.getContentType();
        if (Strings.isNullOrEmpty(contentType)) {
            resp.setStatus(SC_BAD_REQUEST);
        } else {
            try {
                uploadHandler.handle(contentType, req.getInputStream(), resp);
            } catch (IOException | MailboxException e) {
                LOGGER.error("Error while uploading content", e);
                resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
