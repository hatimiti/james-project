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
package org.apache.james.mailbox.store.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;

/**
 * Abstract base class for {@link MessageMapper} implementation
 * which already takes care of most uid / mod-seq handling.
 *
 */
public abstract class AbstractMessageMapper extends TransactionalMapper implements MessageMapper {
    protected final MailboxSession mailboxSession;
    private final UidProvider uidProvider;
    private final ModSeqProvider modSeqProvider;

    public AbstractMessageMapper(MailboxSession mailboxSession, UidProvider uidProvider, ModSeqProvider modSeqProvider) {
        this.mailboxSession = mailboxSession;
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#getHighestModSeq(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long getHighestModSeq(Mailbox mailbox) throws MailboxException {
        return modSeqProvider.highestModSeq(mailboxSession, mailbox);
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#getLastUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long getLastUid(Mailbox mailbox) throws MailboxException {
        return uidProvider.lastUid(mailboxSession, mailbox);
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.model.MessageRange)
     */
    public Iterator<UpdatedFlags> updateFlags(Mailbox mailbox, FlagsUpdateCalculator flagsUpdateCalculator, MessageRange set) throws MailboxException {
        final List<UpdatedFlags> updatedFlags = new ArrayList<UpdatedFlags>();
        Iterator<MailboxMessage> messages = findInMailbox(mailbox, set, FetchType.Metadata, -1);
        
        long modSeq = -1;
        if (messages.hasNext()) {
            // if a mailbox does not support mod-sequences the provider may be null
            if (modSeqProvider != null) {
                modSeq = modSeqProvider.nextModSeq(mailboxSession, mailbox);
            }
        }
        while(messages.hasNext()) {
        	final MailboxMessage member = messages.next();
            Flags originalFlags = member.createFlags();
            member.setFlags(flagsUpdateCalculator.buildNewFlags(originalFlags));
            Flags newFlags = member.createFlags();
            if (UpdatedFlags.flagsChanged(originalFlags, newFlags)) {
                // increase the mod-seq as we changed the flags
                member.setModSeq(modSeq);
                save(mailbox, member);
            }

            
            UpdatedFlags uFlags = new UpdatedFlags(member.getUid(), member.getModSeq(), originalFlags, newFlags);
            
            updatedFlags.add(uFlags);
            
        }

        return updatedFlags.iterator();

    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#add(org.apache.james.mailbox.store.mail.model.Mailbox, MailboxMessage)
     */
    public MessageMetaData add(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        message.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        
        // if a mailbox does not support mod-sequences the provider may be null
        if (modSeqProvider != null) {
            message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
        }
        MessageMetaData data = save(mailbox, message);
       
        return data;
        
    }

    
    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, MailboxMessage)
     */
    public MessageMetaData copy(Mailbox mailbox, MailboxMessage original) throws MailboxException {
        long uid = uidProvider.nextUid(mailboxSession, mailbox);
        long modSeq = -1;
        if (modSeqProvider != null) {
            modSeq = modSeqProvider.nextModSeq(mailboxSession, mailbox);
        }
        final MessageMetaData metaData = copy(mailbox, uid, modSeq, original);  
        
        return metaData;
    }

   
    
    
    /**
     * Save the {@link MailboxMessage} for the given {@link Mailbox} and return the {@link MessageMetaData}
     * 
     * @param mailbox
     * @param message
     * @return metaData
     * @throws MailboxException
     */
    protected abstract MessageMetaData save(Mailbox mailbox, MailboxMessage message) throws MailboxException;

    
    /**
     * Copy the MailboxMessage to the Mailbox, using the given uid and modSeq for the new MailboxMessage
     * 
     * @param mailbox
     * @param uid
     * @param modSeq
     * @param original
     * @return metaData
     * @throws MailboxException
     */
    protected abstract MessageMetaData copy(Mailbox mailbox, long uid, long modSeq, MailboxMessage original) throws MailboxException;
    
}
