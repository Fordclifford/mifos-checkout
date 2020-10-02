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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.accounting.budget.api.BudgetJsonInputParams;
import org.apache.fineract.accounting.budget.command.BudgetCommand;
import org.apache.fineract.accounting.budget.data.BudgetData;
import org.apache.fineract.accounting.budget.domain.Budget;
import org.apache.fineract.accounting.budget.domain.BudgetRepository;
import org.apache.fineract.accounting.budget.exception.AccountUsedException;
import org.apache.fineract.accounting.budget.exception.BudgetInvalidAccountException;
import org.apache.fineract.accounting.budget.exception.BudgetInvalidAccountTypeException;
import org.apache.fineract.accounting.budget.exception.BudgetNotExpenseAcccountException;
import org.apache.fineract.accounting.budget.exception.BudgetNotFoundException;
import org.apache.fineract.accounting.budget.serialization.BudgetCommandFromApiJsonDeserializer;
import org.apache.fineract.accounting.common.AccountingConstants;
import org.apache.fineract.accounting.glaccount.api.GLAccountJsonInputParams;
import org.apache.fineract.accounting.glaccount.command.GLAccountCommand;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.glaccount.exception.GLAccountDisableException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountDuplicateException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidDeleteException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidParentException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidUpdateException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountNotFoundException;
import org.apache.fineract.accounting.glaccount.exception.InvalidParentGLAccountHeadException;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidDeleteException.GL_ACCOUNT_INVALID_DELETE_REASON;
import org.apache.fineract.accounting.glaccount.exception.GLAccountInvalidUpdateException.GL_ACCOUNT_INVALID_UPDATE_REASON;
import org.apache.fineract.accounting.glaccount.serialization.GLAccountCommandFromApiJsonDeserializer;
import org.apache.fineract.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.apache.fineract.accounting.journalentry.data.BudgetTransactionDTO;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.accounting.journalentry.service.CashBasedAccountingProcessorForBudgetTransactions;
import org.apache.fineract.accounting.journalentry.service.CashBasedAccountingProcessorForClientTransactions;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetWritePlatformServiceJpaRepositoryImpl implements  BudgetWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(BudgetWritePlatformServiceJpaRepositoryImpl.class);

    private final BudgetRepository budgetRepository;
    private final GLAccountRepository glAccountRepository;

    private final BudgetCommandFromApiJsonDeserializer fromApiJsonDeserializer;

    private final JdbcTemplate jdbcTemplate;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final PlatformSecurityContext context;
    final BudgetReadPlatformService budgetRead;
    private final CashBasedAccountingProcessorForBudgetTransactions accountingProcessorForBudgetTransactions;
    

    @Autowired
    public BudgetWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context, final BudgetRepository budgetRepository,GLAccountRepository glAccountRepository,
            final BudgetCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final RoutingDataSource dataSource,final BudgetReadPlatformService budgetAcc,  final OfficeRepositoryWrapper officeRepositoryWrapper,final CashBasedAccountingProcessorForBudgetTransactions journalEntryService ) {
        this.budgetRepository = budgetRepository;  
        this.glAccountRepository=glAccountRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
      
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.context=context;
     
        this.budgetRead=budgetAcc;

        this.accountingProcessorForBudgetTransactions=journalEntryService;
        this.officeRepositoryWrapper=officeRepositoryWrapper;

    }

    @Transactional
    @Override
    public CommandProcessingResult createBudget(final JsonCommand command) {
        try {
            final BudgetCommand accountCommand = this.fromApiJsonDeserializer.commandFromApiJson(command.json());
            accountCommand.validateForCreate();

            // check parent is valid
            final Long expenseAccountId = command.longValueOfParameterNamed(BudgetJsonInputParams.EXPENSE_ACCOUNT_ID.getValue());
            final Long assetAccountId = command.longValueOfParameterNamed(BudgetJsonInputParams.ASSET_ACCOUNT_ID.getValue());
            final Long liabilityAccountId = command.longValueOfParameterNamed(BudgetJsonInputParams.LIABILITY_ACCOUNT_ID.getValue());
            final Long cashAccountId = command.longValueOfParameterNamed(BudgetJsonInputParams.CASH_ACCOUNT_ID.getValue());
            final Long officeId = command.longValueOfParameterNamed(BudgetJsonInputParams.OFFICE.getValue());
            final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            final Date transactDate = command.DateValueOfParameterNamed(BudgetJsonInputParams.CREATE_DATE.getValue());
          final BigDecimal  amount = command.bigDecimalValueOfParameterNamed(BudgetJsonInputParams.AMOUNT.getValue());
            
            
            final Long year = command.longValueOfParameterNamed(BudgetJsonInputParams.YEAR.getValue());
            
            GLAccount expenseAccount = null;
            GLAccount cashAccount = null;
            GLAccount liabilityAccount = null;
            GLAccount assetAccount = null;
            if (expenseAccountId != null && liabilityAccountId != null && assetAccountId != null && cashAccountId!=null ) {
            	//expenseAccount = validateParentGLAccount(expenseAccountId);
            	expenseAccount = validateTypeGLAccount(expenseAccountId, GLAccountType.EXPENSE.getValue());
            	cashAccount = validateTypeGLAccount(cashAccountId, GLAccountType.ASSET.getValue());
                           	
            	assetAccount = validateTypeGLAccount(assetAccountId, GLAccountType.ASSET.getValue());
            	liabilityAccount= validateTypeGLAccount(liabilityAccountId, GLAccountType.LIABILITY.getValue());
            	
            	BudgetData checkAsset =budgetRead.retrieveByAsetAccountId(assetAccountId, year);
            	  
            	  	
            	  	if(	checkAsset!=null){
            	  		throw new AccountUsedException(assetAccountId,year);
            	  		
            	  	}
            	            	
            }

            final Budget budgetAccount = Budget.fromJson(office,liabilityAccount,expenseAccount,assetAccount,command,cashAccount);
          
            this.budgetRepository.saveAndFlush(budgetAccount);

           // glAccount.generateHierarchy();

            this.budgetRepository.save(budgetAccount);
System.out.println("budget Id"+budgetAccount.getId());
            
         BudgetTransactionDTO   budgetTransactionDTO = new BudgetTransactionDTO(officeId, budgetAccount.getId(), cashAccountId, assetAccountId, "KE", budgetAccount.getId(), transactDate, null, amount, true, false);
            
            accountingProcessorForBudgetTransactions.createJournalEntriesForBudgetTransaction(budgetTransactionDTO);
            
           return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(budgetAccount.getId()).build();
        } catch (final DataIntegrityViolationException dve) {
           // handleGLAccountDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }

	@Transactional
    @Override
    public CommandProcessingResult updateBudget(final Long glAccountId, final JsonCommand command) {
        try {
            final BudgetCommand accountCommand = this.fromApiJsonDeserializer.commandFromApiJson(command.json());
            accountCommand.validateForUpdate();
//			if (command.hasParameter(GLAccountJsonInputParams.DISABLED
//					.getValue())
//					&& command
//							.booleanPrimitiveValueOfParameterNamed(GLAccountJsonInputParams.DISABLED
//									.getValue())) {
//				validateForAttachedProduct(glAccountId);
//			}
            final Long expenseAcc = command.longValueOfParameterNamed(BudgetJsonInputParams.EXPENSE_ACCOUNT_ID.getValue());
            final Long assetAcc = command.longValueOfParameterNamed(BudgetJsonInputParams.ASSET_ACCOUNT_ID.getValue());
            final Long liabilityeAcc = command.longValueOfParameterNamed(BudgetJsonInputParams.LIABILITY_ACCOUNT_ID.getValue());
           //   if (glAccountId.equals(parentId)) { throw new InvalidParentGLAccountHeadException(glAccountId, parentId); }
            // is the glAccount valid
            final Budget budgetAccount = this.budgetRepository.findOne(glAccountId);
            if (budgetAccount == null) { throw new BudgetNotFoundException(glAccountId); }

            final Map<String, Object> changesOnly = budgetAccount.update(command);

            // is the new parent valid
            if (changesOnly.containsKey(BudgetJsonInputParams.ASSET_ACCOUNT_ID.getValue())) {
                final GLAccount asset = validateParentGLAccount(assetAcc);
               
                budgetAccount.updateAssetAccount(asset);
              
            }
            if (changesOnly.containsKey(BudgetJsonInputParams.EXPENSE_ACCOUNT_ID.getValue())) {
               
                final GLAccount expense = validateParentGLAccount(expenseAcc);
                 budgetAccount.updateExpenseAccount(expense);
              
            }
            if (changesOnly.containsKey(BudgetJsonInputParams.LIABILITY_ACCOUNT_ID.getValue())) {
                final GLAccount liability = validateParentGLAccount(liabilityeAcc);
                budgetAccount.updateLiabilityAccount(liability);
            }

//            if (changesOnly.containsKey(GLAccountJsonInputParams.TAGID.getValue())) {
//                final Long tagIdLongValue = command.longValueOfParameterNamed(GLAccountJsonInputParams.TAGID.getValue());
//                final GLAccountType accountType = GLAccountType.fromInt(glAccount.getType());
//                CodeValue tagID = null;
//                if (tagIdLongValue != null) {
//                    tagID = retrieveTagId(tagIdLongValue, accountType);
//                }
//                glAccount.updateTagId(tagID);
//            }

            /**
             * a detail account cannot be changed to a header account if
             * transactions are already logged against it
             **/
//            if (changesOnly.containsKey(GLAccountJsonInputParams.USAGE.getValue())) {
//                if (glAccount.isHeaderAccount()) {
//                    final List<JournalEntry> journalEntriesForAccount = this.glJournalEntryRepository
//                            .findFirstJournalEntryForAccount(glAccountId);
//                    if (journalEntriesForAccount.size() > 0) { throw new GLAccountInvalidUpdateException(
//                            GL_ACCOUNT_INVALID_UPDATE_REASON.TRANSANCTIONS_LOGGED, glAccountId); }
//                }
//            }

            if (!changesOnly.isEmpty()) {
                this.budgetRepository.saveAndFlush(budgetAccount);
            }

            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(budgetAccount.getId())
                    .with(changesOnly).build();
        } catch (final DataIntegrityViolationException dve) {
           // handleBudgetDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }
	
	
//	public void passEntry(JsonCommand command) {
//				
//		  final Map<String, Object> changes = new LinkedHashMap<>();
//          
//          /** Set a transaction Id and save these Journal entries **/
//          final Long officeId = command.longValueOfParameterNamed(BudgetJsonInputParams.OFFICE.getValue());
//          final Long creditAccount = command.longValueOfParameterNamed(BudgetJsonInputParams.CASH_ACCOUNT_ID.getValue());
//          final Long debitAccount = command.longValueOfParameterNamed(BudgetJsonInputParams.ASSET_ACCOUNT_ID.getValue());
//          
//          final Date transactDate = command.DateValueOfParameterNamed(BudgetJsonInputParams.CREATE_DATE.getValue());
//          
//      	// final LocalDate transactionDate = command.localDateValueOfParameterNamed(JournalEntryJsonInputParams.TRANSACTION_DATE.getValue());
//           final String transactionId = generateTransactionId(officeId);
//          // final String referenceNumber = command.stringValueOfParameterNamed(JournalEntryJsonInputParams.REFERENCE_NUMBER.getValue());
//           final BigDecimal  amount = command.bigDecimalValueOfParameterNamed(BudgetJsonInputParams.AMOUNT.getValue());
//
//      	 final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
//            final String currencyCode = "KE";
//            final PaymentDetail paymentDetail = this.paymentDetailService.createPaymentDetailforBudget();
//            //Long expenseId=budgetReadService.getAccountById(creditEntryCommand.getGlAccountId()).getExpenseAccountId();
//            final GLAccount credit = this.glAccountRepository.findOne(creditAccount);
//            final GLAccount debit = this.glAccountRepository.findOne(debitAccount);
//            if (credit == null) { throw new GLAccountNotFoundException(creditAccount); }
//            if (debit == null) { throw new GLAccountNotFoundException(debitAccount); }
//                
//      	final JournalEntry glJournal= JournalEntry.createNew(office, paymentDetail, credit, currencyCode, transactionId,
//                  true, transactDate, JournalEntryType.CREDIT, amount, "Migrating initial budget amount", null, null, null,
//                  null, null, null, null); 
//      	
//      	final JournalEntry glJournal1= JournalEntry.createNew(office, paymentDetail, debit, currencyCode, transactionId,
//                true, transactDate, JournalEntryType.DEBIT, amount, "Migrating initial budget amount", null, null, null,
//                null, null, null, null);
//        this.glJournalEntryRepository.saveAndFlush(glJournal);
//        this.glJournalEntryRepository.saveAndFlush(glJournal1);
//		
//		
//	}
	
	   private String generateTransactionId(final Long officeId) {
	        final AppUser user = this.context.authenticatedUser();
	        final Long time = System.currentTimeMillis();
	        final String uniqueVal = String.valueOf(time) + user.getId() + officeId;
	        final String transactionId = Long.toHexString(Long.parseLong(uniqueVal));
	        return transactionId;
	    }

    private void validateForAttachedProduct(Long glAccountId) {
		String sql = "select count(*) from acc_product_mapping acc where acc.gl_account_id = "+glAccountId;
		int count = this.jdbcTemplate.queryForObject(sql, Integer.class);
		if(count>0){
			throw new GLAccountDisableException();
		}
	}

	@Transactional
    @Override
    public CommandProcessingResult deleteBudget(final Long glAccountId) {
        final Budget budgetAccount = this.budgetRepository.findOne(glAccountId);

        if (budgetAccount == null) { throw new BudgetNotFoundException(glAccountId); }

        // validate this isn't a header account that has children
//        if (glAccount.isHeaderAccount() && glAccount.getChildren().size() > 0) { throw new GLAccountInvalidDeleteException(
//                GL_ACCOUNT_INVALID_DELETE_REASON.HAS_CHILDREN, glAccountId); }
//
//        // does this account have transactions logged against it
//        final List<JournalEntry> journalEntriesForAccount = this.glJournalEntryRepository.findFirstJournalEntryForAccount(glAccountId);
//        if (journalEntriesForAccount.size() > 0) { throw new GLAccountInvalidDeleteException(
//                GL_ACCOUNT_INVALID_DELETE_REASON.TRANSANCTIONS_LOGGED, glAccountId); }
        this.budgetRepository.delete(budgetAccount);

        return new CommandProcessingResultBuilder().withEntityId(glAccountId).build();
    }

    /**
     * @param command
     * @return
     */
    private GLAccount validateParentGLAccount(final Long parentAccountId) {
        GLAccount parentGLAccount = null;
        if (parentAccountId != null) {
            parentGLAccount = this.glAccountRepository.findOne(parentAccountId);
            if (parentGLAccount == null) { throw new GLAccountNotFoundException(parentAccountId); }
            
            if (parentGLAccount.getType()!=5) { throw new BudgetNotExpenseAcccountException(parentAccountId); }
          
            // ensure parent is not a header account
            if (parentGLAccount.isHeaderAccount()) { throw new BudgetInvalidAccountException(parentAccountId); }
       }
        return parentGLAccount;
    }
    
    private GLAccount validateTypeGLAccount(final Long accountId,Integer type) {
        GLAccount account= null;
        if (accountId != null) {
        	account = this.glAccountRepository.findOne(accountId);
            if (account == null) { throw new GLAccountNotFoundException(accountId); }
            
            if (account.getType()!=type) { throw new BudgetInvalidAccountTypeException(account.getId(),GLAccountType.fromInt(account.getType()).getCode(),GLAccountType.fromInt(type).getCode()); }
            
          
            // ensure parent is not a header account
            if (account.isHeaderAccount()) { throw new BudgetInvalidAccountException(accountId); }
       }
        return account;
    }

    /**
     * @param command
     * @param dve
     */
//    private void handleBudgetDataIntegrityIssues(final JsonCommand command, final DataIntegrityViolationException dve) {
//        final Throwable realCause = dve.getMostSpecificCause();
//        if (realCause.getMessage().contains("acc_gl_code")) {
//            final String glCode = command.stringValueOfParameterNamed(GLAccountJsonInputParams.GL_CODE.getValue());
//            throw new GLAccountDuplicateException(glCode);
//        }
//
//        logger.error(dve.getMessage(), dve);
//        throw new PlatformDataIntegrityException("error.msg.glAccount.unknown.data.integrity.issue",
//                "Unknown data integrity issue with resource GL Account: " + realCause.getMessage());
//    }

//    private CodeValue retrieveTagId(final Long tagId, final GLAccountType accountType) {
//        CodeValue glAccountTagType = null;
//        if (accountType.isAssetType()) {
//            glAccountTagType = this.codeValueRepositoryWrapper.findOneByCodeNameAndIdWithNotFoundDetection(
//                    AccountingConstants.ASSESTS_TAG_OPTION_CODE_NAME, tagId);
//        } else if (accountType.isLiabilityType()) {
//            glAccountTagType = this.codeValueRepositoryWrapper.findOneByCodeNameAndIdWithNotFoundDetection(
//                    AccountingConstants.LIABILITIES_TAG_OPTION_CODE_NAME, tagId);
//        } else if (accountType.isEquityType()) {
//            glAccountTagType = this.codeValueRepositoryWrapper.findOneByCodeNameAndIdWithNotFoundDetection(
//                    AccountingConstants.EQUITY_TAG_OPTION_CODE_NAME, tagId);
//        } else if (accountType.isIncomeType()) {
//            glAccountTagType = this.codeValueRepositoryWrapper.findOneByCodeNameAndIdWithNotFoundDetection(
//                    AccountingConstants.INCOME_TAG_OPTION_CODE_NAME, tagId);
//        } else if (accountType.isExpenseType()) {
//            glAccountTagType = this.codeValueRepositoryWrapper.findOneByCodeNameAndIdWithNotFoundDetection(
//                    AccountingConstants.EXPENSES_TAG_OPTION_CODE_NAME, tagId);
//        }
//        return glAccountTagType;
//    }

}
