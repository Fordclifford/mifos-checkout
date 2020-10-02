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
package org.apache.fineract.accounting.budget.serialization;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.budget.api.BudgetJsonInputParams;
import org.apache.fineract.accounting.budget.command.BudgetCommand;
import org.apache.fineract.accounting.glaccount.api.GLAccountJsonInputParams;
import org.apache.fineract.accounting.glaccount.command.GLAccountCommand;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.guarantor.command.GuarantorCommand;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of {@link FromApiJsonDeserializer} for
 * {@link GuarantorCommand}'s.
 */
@Component
public final class BudgetCommandFromApiJsonDeserializer extends AbstractFromApiJsonDeserializer<BudgetCommand> {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public BudgetCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonfromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonfromApiJsonHelper;
    }

    @Override
    public BudgetCommand commandFromApiJson(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        final Set<String> supportedParameters = BudgetJsonInputParams.getAllValues();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, supportedParameters);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long id = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.ID.getValue(), element);
        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(BudgetJsonInputParams.AMOUNT.getValue(), element);
        final String name = this.fromApiJsonHelper.extractStringNamed(BudgetJsonInputParams.AMOUNT.getValue(), element);
        final String description = this.fromApiJsonHelper.extractStringNamed(BudgetJsonInputParams.DESCRIPTION.getValue(), element);
        final Boolean disabled = this.fromApiJsonHelper.extractBooleanNamed(BudgetJsonInputParams.DISABLED.getValue(), element);
        
        final Long year = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.YEAR.getValue(), element);
        final LocalDate fromDate = this.fromApiJsonHelper.extractLocalDateNamed(BudgetJsonInputParams.FROM_DATE.getValue(), element);
        final LocalDate toDate = this.fromApiJsonHelper.extractLocalDateNamed(BudgetJsonInputParams.TO_DATE.getValue(), element);
        final LocalDate createDate = this.fromApiJsonHelper.extractLocalDateNamed(BudgetJsonInputParams.CREATE_DATE.getValue(), element);
        
        
        final Long cashAccount = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.CASH_ACCOUNT_ID.getValue(), element);
        final Long expenseAccount = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.EXPENSE_ACCOUNT_ID.getValue(), element);
        final Long assetAcount = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.ASSET_ACCOUNT_ID.getValue(), element);
        final Long liabilityAccount = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.LIABILITY_ACCOUNT_ID.getValue(), element);
        final Long officeId = this.fromApiJsonHelper.extractLongNamed(BudgetJsonInputParams.OFFICE.getValue(), element);
        
       
        return new BudgetCommand(officeId,id,amount,expenseAccount,liabilityAccount,assetAcount,disabled,description,name,fromDate,toDate,createDate,year,cashAccount);
    }
}