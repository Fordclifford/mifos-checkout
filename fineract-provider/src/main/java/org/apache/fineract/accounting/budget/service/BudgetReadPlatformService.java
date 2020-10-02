/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.budget.service;

import java.util.Date;
import java.util.List;

import org.apache.fineract.accounting.budget.data.BudgetData;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;



public interface BudgetReadPlatformService {

    List<BudgetData> retrieveAll(Integer accountId, String searchParam);

    BudgetData retrieveBudgetById(long budgetId);
    BudgetData retrieveAccountById(long accountId);
    BudgetData retrieveNewBudgetDetails();

	BudgetData getAccountById(long accountId);

	
	BudgetData retrieveByAsetAccountId(long accountId, Long year);

	BudgetData getExpenseAccountById(long accountId);

	BudgetData getCashAccountById(long accountId);
    
    
  

//    List<GLAccountDataForLookup> retrieveAccountsByTagId(final Long ruleId, final Integer transactionType);
}