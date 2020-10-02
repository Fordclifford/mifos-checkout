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
package org.apache.fineract.accounting.journalentry.data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.domain.ClientTransactionType;

public class BudgetTransactionDTO {

    private final Long officeId;
    private final Long budgetId;
    
    private final Long creditAccountId;
    
    private final Long debitAccountId;
    
    private String currencyCode;

    private final Long transactionId;
    private final Date transactionDate;
    private final Long paymentTypeId;
   private final BigDecimal amount;

    private final Boolean accountingEnabled;

    /*** Boolean values determines if the transaction is reversed ***/
    private final boolean reversed;



    public BudgetTransactionDTO(Long officeId, Long budgetId, Long creditAccountId, Long debitAccountId,
			String currencyCode, Long transactionId, Date transactionDate, Long paymentTypeId,
			 BigDecimal amount, Boolean accountingEnabled, boolean reversed) {
		super();
		this.officeId = officeId;
		this.budgetId = budgetId;
		this.creditAccountId = creditAccountId;
		this.debitAccountId = debitAccountId;
		this.currencyCode = currencyCode;
		this.transactionId = transactionId;
		this.transactionDate = transactionDate;
		this.paymentTypeId = paymentTypeId;
		
		this.amount = amount;
		this.accountingEnabled = accountingEnabled;
		this.reversed = reversed;
	}

	public Long getOfficeId() {
        return this.officeId;
    }

    public Date getTransactionDate() {
        return this.transactionDate;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public boolean isReversed() {
        return this.reversed;
    }

    public Long getPaymentTypeId() {
        return this.paymentTypeId;
    }

  
//    public boolean isBudgetPayment() {
//        return ClientTransactionType.PAY_CHARGE.getValue().equals(new Integer(this.transactionType.getId().intValue()));
//    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

//    public List<ClientChargePaymentDTO> getChargePayments() {
//        return this.chargePayments;
//    }

    public Long getTransactionId() {
        return this.transactionId;
    }

    public Boolean getAccountingEnabled() {
        return this.accountingEnabled;
    }

	public Long getBudgetId() {
		return budgetId;
	}

	public Long getCreditAccountId() {
		return creditAccountId;
	}

	public Long getDebitAccountId() {
		return debitAccountId;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

  

}
