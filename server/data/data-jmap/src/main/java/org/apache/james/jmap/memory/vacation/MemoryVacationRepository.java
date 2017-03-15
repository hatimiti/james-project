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

package org.apache.james.jmap.memory.vacation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.james.jmap.api.vacation.AccountId;
import org.apache.james.jmap.api.vacation.Vacation;
import org.apache.james.jmap.api.vacation.VacationRepository;
import org.apache.james.jmap.api.vacation.VacationPatch;

import com.google.common.base.Preconditions;

public class MemoryVacationRepository implements VacationRepository {

    private final Map<AccountId, Vacation> vacationMap;

    public MemoryVacationRepository() {
        this.vacationMap = new HashMap<>();
    }

    @Override
    public CompletableFuture<Vacation> retrieveVacation(AccountId accountId) {
        Preconditions.checkNotNull(accountId);
        return CompletableFuture.completedFuture(vacationMap.getOrDefault(accountId, DEFAULT_VACATION));
    }

    @Override
    public CompletableFuture<Void> modifyVacation(AccountId accountId, VacationPatch vacationPatch) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(vacationPatch);
        Vacation oldVacation = retrieveVacation(accountId).join();
        vacationMap.put(accountId, vacationPatch.patch(oldVacation));
        return CompletableFuture.completedFuture(null);
    }


}