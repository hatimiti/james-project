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

package org.apache.james.jmap.methods;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.exceptions.MailboxHasChildException;
import org.apache.james.jmap.exceptions.SystemMailboxNotUpdatableException;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMailboxesRequest;
import org.apache.james.jmap.model.SetMailboxesResponse;
import org.apache.james.jmap.model.SetMailboxesResponse.Builder;
import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.jmap.utils.SortingHierarchicalCollections;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class SetMailboxesDestructionProcessor implements SetMailboxesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMailboxesDestructionProcessor.class);

    private final MailboxManager mailboxManager;
    private final SortingHierarchicalCollections<Map.Entry<String, Mailbox>, String> sortingHierarchicalCollections;
    private final MailboxUtils mailboxUtils;

    @Inject
    @VisibleForTesting
    SetMailboxesDestructionProcessor(MailboxManager mailboxManager, MailboxUtils mailboxUtils) {
        this.mailboxManager = mailboxManager;
        this.sortingHierarchicalCollections =
            new SortingHierarchicalCollections<>(
                    Entry::getKey,
                    x -> x.getValue().getParentId());
        this.mailboxUtils = mailboxUtils;
    }

    public SetMailboxesResponse process(SetMailboxesRequest request, MailboxSession mailboxSession) {
        ImmutableMap<String, Mailbox> idToMailbox = mapDestroyRequests(request, mailboxSession);

        SetMailboxesResponse.Builder builder = SetMailboxesResponse.builder();
        sortingHierarchicalCollections.sortFromLeafToRoot(idToMailbox.entrySet())
            .forEach(entry -> destroyMailbox(entry, mailboxSession, builder));

        notDestroyedRequests(request, idToMailbox, builder);
        return builder.build();
    }

    private ImmutableMap<String, Mailbox> mapDestroyRequests(SetMailboxesRequest request, MailboxSession mailboxSession) {
        ImmutableMap.Builder<String, Mailbox> idToMailboxBuilder = ImmutableMap.builder(); 
        request.getDestroy().stream()
            .map(id -> mailboxUtils.mailboxFromMailboxId(id, mailboxSession))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(mailbox -> idToMailboxBuilder.put(mailbox.getId(), mailbox));
        return idToMailboxBuilder.build();
    }

    private void notDestroyedRequests(SetMailboxesRequest request, ImmutableMap<String, Mailbox> idToMailbox, SetMailboxesResponse.Builder builder) {
        request.getDestroy().stream()
            .filter(id -> !idToMailbox.containsKey(id))
            .forEach(id -> notDestroy(id, builder));
    }

    private void destroyMailbox(Entry<String, Mailbox> entry, MailboxSession mailboxSession, SetMailboxesResponse.Builder builder) {
        try {
            Mailbox mailbox = entry.getValue();
            preconditions(mailbox, mailboxSession);

            mailboxManager.deleteMailbox(mailboxUtils.getMailboxPath(mailbox, mailboxSession), mailboxSession);
            builder.destroyed(entry.getKey());
        } catch (MailboxHasChildException e) {
            builder.notDestroyed(entry.getKey(), SetError.builder()
                    .type("mailboxHasChild")
                    .description(String.format("The mailbox '%s' has a child.", entry.getKey()))
                    .build());
        } catch (SystemMailboxNotUpdatableException e) {
            builder.notDestroyed(entry.getKey(), SetError.builder()
                    .type("invalidArguments")
                    .description(String.format("The mailbox '%s' is a system mailbox.", entry.getKey()))
                    .build());
        } catch (MailboxException e) {
            String message = String.format("An error occurred when deleting the mailbox '%s'", entry.getKey());
            LOGGER.error(message, e);
            builder.notDestroyed(entry.getKey(), SetError.builder()
                    .type("anErrorOccurred")
                    .description(message)
                    .build());
        }
    }

    private void preconditions(Mailbox mailbox, MailboxSession mailboxSession) throws MailboxHasChildException, SystemMailboxNotUpdatableException, MailboxException {
        checkForChild(mailbox.getId(), mailboxSession);
        checkRole(mailbox.getRole());
    }

    private void checkForChild(String id, MailboxSession mailboxSession) throws MailboxHasChildException, MailboxException {
        if (mailboxUtils.hasChildren(id, mailboxSession)) {
            throw new MailboxHasChildException();
        }
    }

    private void checkRole(Optional<Role> role) throws SystemMailboxNotUpdatableException {
        if (role.map(Role::isSystemRole).orElse(false)) {
            throw new SystemMailboxNotUpdatableException();
        }
    }

    private void notDestroy(String id, Builder builder) {
        builder.notDestroyed(id, SetError.builder()
                .type("notFound")
                .description(String.format("The mailbox '%s' was not found.", id))
                .build());
    }
}
