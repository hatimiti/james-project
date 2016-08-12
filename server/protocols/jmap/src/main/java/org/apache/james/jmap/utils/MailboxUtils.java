/****************************************************************
O * Licensed to the Apache Software Foundation (ASF) under one   *
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

package org.apache.james.jmap.utils;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.model.mailbox.SortOrder;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class MailboxUtils {

    private static final boolean DONT_RESET_RECENT = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxUtils.class);
    private static final String WILDCARD = "%";

    private final MailboxManager mailboxManager;
    private final MailboxMapperFactory mailboxMapperFactory;

    @Inject
    @VisibleForTesting
    public MailboxUtils(MailboxManager mailboxManager, MailboxMapperFactory mailboxMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
    }

    public Optional<Mailbox> mailboxFromMailboxPath(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            Optional<Role> role = Role.from(mailboxPath.getName());
            MessageManager.MetaData mailboxMetaData = getMailboxMetaData(mailboxPath, mailboxSession);
            String mailboxId = getMailboxId(mailboxPath, mailboxSession);
            return Optional.ofNullable(Mailbox.builder()
                    .id(mailboxId)
                    .name(getName(mailboxPath, mailboxSession))
                    .parentId(getParentIdFromMailboxPath(mailboxPath, mailboxSession))
                    .role(role)
                    .unreadMessages(mailboxMetaData.getUnseenCount())
                    .totalMessages(mailboxMetaData.getMessageCount())
                    .sortOrder(SortOrder.getSortOrder(role))
                    .build());
        } catch (MailboxException e) {
            LOGGER.warn("Cannot find mailbox for :" + mailboxPath.getName(), e);
            return Optional.empty();
        }
    }

    private String getMailboxId(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        return mailboxMapperFactory.getMailboxMapper(mailboxSession)
                .findMailboxByPath(mailboxPath)
                .getMailboxId()
                .serialize();
    }

    private MessageManager.MetaData getMailboxMetaData(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        return mailboxManager.getMailbox(mailboxPath, mailboxSession)
                .getMetaData(DONT_RESET_RECENT, mailboxSession, MessageManager.MetaData.FetchGroup.UNSEEN_COUNT);
    }

    @VisibleForTesting String getName(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        String name = mailboxPath.getName();
        if (name.contains(String.valueOf(mailboxSession.getPathDelimiter()))) {
            List<String> levels = Splitter.on(mailboxSession.getPathDelimiter()).splitToList(name);
            return levels.get(levels.size() - 1);
        }
        return name;
    }

    public Optional<String> getMailboxNameFromId(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        return getMailboxFromId(mailboxId, mailboxSession)
                .map(org.apache.james.mailbox.store.mail.model.Mailbox::getName);
    }

    private Optional<org.apache.james.mailbox.store.mail.model.Mailbox> getMailboxFromId(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        return mailboxMapperFactory.getMailboxMapper(mailboxSession)
                .findMailboxWithPathLike(new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), WILDCARD))
                .stream()
                .filter(mailbox -> mailbox.getMailboxId().serialize().equals(mailboxId))
                .findFirst();
    }

    @VisibleForTesting String getParentIdFromMailboxPath(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        List<MailboxPath> levels = mailboxPath.getHierarchyLevels(mailboxSession.getPathDelimiter());
        if (levels.size() <= 1) {
            return null;
        }
        MailboxPath parent = levels.get(levels.size() - 2);
        return getMailboxId(parent, mailboxSession);
    }

    public Optional<Mailbox> mailboxFromMailboxId(String mailboxId, MailboxSession mailboxSession) {
        try {
            return getMailboxFromId(mailboxId, mailboxSession)
                .flatMap(jamesMailbox ->
                    mailboxFromMailboxPath(new MailboxPath(jamesMailbox.getNamespace(), mailboxSession.getUser().getUserName(), jamesMailbox.getName()), 
                            mailboxSession)
                );
        } catch (MailboxException e) {
            return Optional.empty();
        }
    }

    public MailboxPath getMailboxPath(Mailbox mailbox, MailboxSession mailboxSession) {
        return new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), getMailboxName(mailbox, mailboxSession));
    }

    private String getMailboxName(Mailbox mailbox, MailboxSession mailboxSession) {
        if (mailbox.getParentId().isPresent()) {
            return getMailboxName(mailboxFromMailboxId(mailbox.getParentId().get(), mailboxSession).get(), mailboxSession) +
                    mailboxSession.getPathDelimiter() + mailbox.getName();
        }
        return mailbox.getName();
    }

    public boolean hasChildren(String mailboxId, MailboxSession mailboxSession) throws MailboxException {
        return getMailboxFromId(mailboxId, mailboxSession)
                .map(Throwing.function(mailbox -> 
                    mailboxMapperFactory.getMailboxMapper(mailboxSession).hasChildren(mailbox, mailboxSession.getPathDelimiter())))
                .orElse(false);
    }

    public Optional<MailboxPath> mailboxPathFromMailboxId(String mailboxId, MailboxSession mailboxSession) {
        Preconditions.checkState(!Strings.isNullOrEmpty(mailboxId), "'mailboxId' is mandatory");
        Preconditions.checkState(mailboxSession != null, "'mailboxId' is mandatory");
        return mailboxFromMailboxId(mailboxId, mailboxSession)
                .map(mailbox -> getMailboxPath(mailbox, mailboxSession));
    }
}
