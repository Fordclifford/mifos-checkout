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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.budget.api.BudgetJsonInputParams;
import org.apache.fineract.accounting.budget.data.BudgetData;
import org.apache.fineract.accounting.budget.domain.Budget;
import org.apache.fineract.accounting.budget.exception.BudgetNotFoundException;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.accounting.glaccount.data.GLAccountDataForLookup;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.glaccount.domain.GLAccountUsage;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidClassificationException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountNotFoundException;
import org.apache.fineract.accounting.glaccount.service.GLAccountReadPlatformService;
import org.apache.fineract.accounting.glaccount.service.GLAccountReadPlatformServiceImpl;
import org.apache.fineract.accounting.journalentry.data.JournalEntryAssociationParametersData;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class BudgetReadPlatformServiceImpl implements BudgetReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final GLAccountRepository glAccountRepository;
    //private final static GLAccountRepository lglAccountRepository;
    private final static String nameDecoratedBaseOnHierarchy = "concat(substring('........................................', 1, ((LENGTH(hierarchy) - LENGTH(REPLACE(hierarchy, '.', '')) - 1) * 4)), name)";

    @Autowired
    public BudgetReadPlatformServiceImpl(final RoutingDataSource dataSource,GLAccountRepository glAccountRepository ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.glAccountRepository=glAccountRepository;
    }
    
     private static final class BudgetMapper implements RowMapper<BudgetData> {
    	  private final GLAccountRepository glAccountRepository;

      //  private final JournalEntryAssociationParametersData associationParametersData;
    	  @Autowired
    	    public BudgetMapper(GLAccountRepository glAccountRepository ) {    	       
    	        this.glAccountRepository=glAccountRepository;
    	    }

//     		public BudgetMapper() {
//		// TODO Auto-generated constructor stub
//	}

			public String schema() {
            StringBuilder sb = new StringBuilder();
            sb.append(" b.id,b.office_id as officeId,b.disabled,b.create_date as createDate,b.year, b.expense_account_id as expenseAccountId,b.asset_account_id as assetAccountId,b.cash_account_id as cashAccountId,b.liability_account_id as liabilityAccountId, b.amount, b.description,b.name,(SELECT NAME FROM acc_gl_account g WHERE g.id=b.liability_account_id) AS liabilityAccountName,(SELECT NAME FROM acc_gl_account g WHERE g.id=b.expense_account_id) AS expenseAccountName,(SELECT NAME FROM acc_gl_account g WHERE g.id=b.asset_account_id) AS assetAccountName,(SELECT NAME FROM acc_gl_account g WHERE g.id=b.cash_account_id) AS cashAccountName,b.from_date as fromDate,b.to_date as toDate ");
//            if (this.associationParametersData.isRunningBalanceRequired()) {
//                sb.append(",gl_j.organization_running_balance as organizationRunningBalance ");
//            }
            sb.append(" from gl_acc_budget b ");
//            if (this.associationParametersData.isRunningBalanceRequired()) {
//                sb.append("left outer Join acc_gl_journal_entry gl_j on gl_j.account_id = gl.id");
//            }
            return sb.toString();
        }

        @Override
        public BudgetData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

           final Long id = rs.getLong("id");
           final Long expenseAccountId = JdbcSupport.getLong(rs, "expenseAccountId");
           final Long cashAccountId = JdbcSupport.getLong(rs, "cashAccountId");
           final Long year = JdbcSupport.getLong(rs, "year");
           final Long assetAccountId = JdbcSupport.getLong(rs, "assetAccountId");
           final Long liabilityAccountId = JdbcSupport.getLong(rs, "liabilityAccountId");
           final Long officeId = JdbcSupport.getLong(rs, "officeId");
           final BigDecimal amount = rs.getBigDecimal("amount");
           final String name = rs.getString("name"); 
           final String description = rs.getString("description"); 
           final Boolean disabled = rs.getBoolean("disabled");     
                   
           final LocalDate createDate = JdbcSupport.getLocalDate(rs, "createDate");
           final LocalDate fromDate = JdbcSupport.getLocalDate(rs, "fromDate");
           final LocalDate toDate = JdbcSupport.getLocalDate(rs, "toDate");
           
                    
           final String assetAccountName = rs.getString("assetAccountName"); 
           final String cashAccountName = rs.getString("cashAccountName"); 
           final String expenseAccountName = rs.getString("expenseAccountName"); 
           final String liabilityAccountName = rs.getString("liabilityAccountName"); 
         
                  return new BudgetData(officeId,expenseAccountName, liabilityAccountName, assetAccountName, id, amount, expenseAccountId, liabilityAccountId, assetAccountId, disabled, description, name,fromDate,toDate,year,createDate,cashAccountId,cashAccountName);
        
    }
    }

    @Override
    public List<BudgetData> retrieveAll(final Integer accountId,String searchParam) {
       


        final BudgetMapper rm = new BudgetMapper(glAccountRepository);
        String sql = "select " + rm.schema();
        final Object[] paramaterArray = new Object[3];
        int arrayPos = 0;
        boolean filtersPresent = false;
        if ( StringUtils.isNotBlank(searchParam)) {
            filtersPresent = true;
            sql += " where";
        }

        if (filtersPresent) {
            boolean firstWhereConditionAdded = false;
            if (accountId != null) {
                sql += " expense_account_id like ?";
                paramaterArray[arrayPos] = accountId;
                arrayPos = arrayPos + 1;
                firstWhereConditionAdded = true;
            }
            if (StringUtils.isNotBlank(searchParam)) {
                if (firstWhereConditionAdded) {
                    sql += " and ";
                }
                sql += " (description like %?% or name like %?% or liability_account_id like %?% or expense_account_id like %?% or asset_account_id like %?% or disabled like %?% or amount like %?% )";
                paramaterArray[arrayPos] = searchParam;
                arrayPos = arrayPos + 1;
                paramaterArray[arrayPos] = searchParam;
                arrayPos = arrayPos + 1;
                firstWhereConditionAdded = true;
            }
        }

        sql+=" ORDER BY b.id ASC";
        //System.out.println(sql);

        final Object[] finalObjectArray = Arrays.copyOf(paramaterArray, arrayPos);
        return this.jdbcTemplate.query(sql, rm, finalObjectArray);
    }

    @Override
    public BudgetData retrieveBudgetById(final long budgetId) {
        try {

            final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.id = ?");
            
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { budgetId });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
            throw new BudgetNotFoundException(budgetId);
        }
    }
    


    
    @Override
    public BudgetData retrieveAccountById(final long accountId) {
        try {
        	
        //	System.out.println("db acccount Id "+accountId);
        	
        	
            final GLAccount glAccount = this.glAccountRepository.findOne(accountId);
            if (glAccount == null ) { throw new GLAccountNotFoundException(accountId); }
            


            final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.expense_account_id = ?");
           // System.out.println("query"+sql.toString());
            
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { accountId });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
            throw new BudgetNotFoundException(accountId);
        }
    }
    
    @Override
    public BudgetData retrieveByAsetAccountId(final long accountId,Long year) {
        try {
        	
        //	System.out.println("db acccount Id "+accountId);
        	
        	
            final GLAccount glAccount = this.glAccountRepository.findOne(accountId);
            if (glAccount == null ) { throw new GLAccountNotFoundException(accountId); }
            


            final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.asset_account_id = ? and year = ? ");
            System.out.println("query"+sql.toString());
            
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { accountId,year });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public BudgetData getExpenseAccountById(final long accountId) {
        try {
        	
        	final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.expense_account_id = ?");
           System.out.println("query"+sql.toString());
            
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { accountId });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
           return null;
        }
    }
    
    
    @Override
    public BudgetData getAccountById(final long accountId) {
        try {
        	
                  final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.asset_account_id = ? order by id desc limit 1");
           
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { accountId });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
           return null;
        }
    }
    
    @Override
    public BudgetData getCashAccountById(final long accountId) {
        try {
        	
                  final BudgetMapper rm = new BudgetMapper(glAccountRepository);
            final StringBuilder sql = new StringBuilder();
            sql.append("select ").append(rm.schema());
           
            sql.append("where b.cash_account_id = ? order by id desc limit 1");
           
            final BudgetData glAccountData = this.jdbcTemplate.queryForObject(sql.toString(), rm, new Object[] { accountId });

            return glAccountData;
        } catch (final EmptyResultDataAccessException e) {
           return null;
        }
    }

	@Override
	public BudgetData retrieveNewBudgetDetails() {
		 return BudgetData.sensibleDefaultsForNewBudgetCreation();
	}


}