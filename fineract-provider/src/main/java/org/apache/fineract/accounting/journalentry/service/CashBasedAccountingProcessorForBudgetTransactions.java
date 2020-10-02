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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.fineract.accounting.closure.domain.GLClosure;
import org.apache.fineract.accounting.journalentry.data.BudgetTransactionDTO;
import org.apache.fineract.organisation.office.domain.Office;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CashBasedAccountingProcessorForBudgetTransactions implements AccountingProcessorForBudgetTransactions {

    AccountingProcessorHelper helper;

    @Autowired
    public CashBasedAccountingProcessorForBudgetTransactions(final AccountingProcessorHelper accountingProcessorHelper) {
        this.helper = accountingProcessorHelper;
    }

    @Override
    public void createJournalEntriesForBudgetTransaction(BudgetTransactionDTO budgetTransactionDTO) {
        if (budgetTransactionDTO.getAccountingEnabled()) {
            final GLClosure latestGLClosure = this.helper.getLatestClosureByBranch(budgetTransactionDTO.getOfficeId());
            final Date transactionDate = budgetTransactionDTO.getTransactionDate();
            final Office office = this.helper.getOfficeById(budgetTransactionDTO.getOfficeId());
            this.helper.checkForBranchClosures(latestGLClosure, transactionDate);

//            /** Handle client payments **/
          //  if (budgetTransactionDTO.isChargePayment()) {
            createJournalEntriesForBudget(budgetTransactionDTO, office);
          //  }
        }
    }

    /**
     * Create a single debit to fund source and multiple credits for the income
     * account mapped with each charge this payment pays off
     * 
     * In case the loan transaction is a reversal, all debits are turned into
     * credits and vice versa
     */
    private void createJournalEntriesForBudget(final BudgetTransactionDTO budgetTransactionDTO, final Office office) {
        // client properties
      //  final Long clientId = budgetTransactionDTO.getClientId();

        // transaction properties
        final String currencyCode = budgetTransactionDTO.getCurrencyCode();
        final String transactionId = budgetTransactionDTO.getTransactionId().toString();
        final Date transactionDate = budgetTransactionDTO.getTransactionDate();
        final BigDecimal amount = budgetTransactionDTO.getAmount();
        final boolean isReversal = budgetTransactionDTO.isReversed();
        final Long creditAccountId=budgetTransactionDTO.getCreditAccountId();
        final Long debitAccountId=budgetTransactionDTO.getDebitAccountId();
        final Long budgetId=budgetTransactionDTO.getBudgetId();


        if (amount != null && !(amount.compareTo(BigDecimal.ZERO) == 0)) {
            BigDecimal totalCreditedAmount = this.helper.createCreditJournalEntryOrReversalForBudget(office,currencyCode,creditAccountId,budgetId,transactionId,transactionDate,isReversal,budgetTransactionDTO);

            /***
             * create a single Debit entry (or reversal) for the entire amount
             * that was credited (accounting is turned on at the level of for
             * each charge that has been paid by this transaction)
             **/
            this.helper.createDebitJournalEntryOrReversalForBudget(office, currencyCode, budgetId,debitAccountId,transactionId,transactionDate,totalCreditedAmount,isReversal);
        }

    }

}
