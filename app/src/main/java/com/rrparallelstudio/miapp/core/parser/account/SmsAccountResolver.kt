package com.rrparallelstudio.miapp.core.parser.account

import com.rrparallelstudio.miapp.core.database.dao.AccountDao
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.parser.model.ParsedSmsCandidate
import com.rrparallelstudio.miapp.core.parser.model.SmsInstrument
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAccountResolver
    @Inject
    constructor(
        private val accountDao: AccountDao,
    ) {
        /** Maps SMS to an existing account only; never inserts. Used when queueing pending approvals. */
        suspend fun findExistingAccountId(candidate: ParsedSmsCandidate): Long? {
            val accounts = accountDao.getActiveAccountsOnce()
            return resolveAccountId(accounts, candidate, createIfMissing = false)
        }

        /** Maps SMS to an account, creating credit-card or wallet rows when needed. Used on direct approval only. */
        suspend fun provisionAccountId(candidate: ParsedSmsCandidate): Long? {
            val accounts = accountDao.getActiveAccountsOnce()
            return resolveAccountId(accounts, candidate, createIfMissing = true)
        }

        private suspend fun resolveAccountId(
            accounts: List<AccountEntity>,
            candidate: ParsedSmsCandidate,
            createIfMissing: Boolean,
        ): Long? =
            when {
                SmsSpendChannel.isCreditCardSpend(candidate) -> {
                    val last4 = candidate.accountLastDigits ?: return null
                    findOrCreateCreditCard(accounts, last4, createIfMissing)
                }
                SmsSpendChannel.isPluxeeMealSpend(candidate) ->
                    findOrCreateWallet(accounts, WALLET_PLUXEE_NAME, createIfMissing)
                SmsSpendChannel.isHpPayWalletSpend(candidate) ->
                    findOrCreateWallet(accounts, WALLET_HP_PAY_NAME, createIfMissing)
                candidate.instrument == SmsInstrument.SAVINGS && !candidate.accountLastDigits.isNullOrBlank() -> {
                    findBankByLastFour(accounts, candidate.accountLastDigits!!)
                }
                else -> null
            }

        private suspend fun findOrCreateCreditCard(
            accounts: List<AccountEntity>,
            last4: String,
            createIfMissing: Boolean,
        ): Long? {
            val existing =
                accounts.firstOrNull {
                    it.type == AccountType.CREDIT && it.cardLastFour == last4
                } ?: accounts.firstOrNull {
                    it.type == AccountType.CREDIT && it.name.contains(last4)
                }
            if (existing != null) return existing.id
            if (!createIfMissing) return null
            val name = "Credit card • $last4"
            return accountDao.insert(
                AccountEntity(
                    name = name,
                    type = AccountType.CREDIT,
                    cardLastFour = last4,
                    smsAutoCreated = true,
                ),
            )
        }

        private suspend fun findOrCreateWallet(
            accounts: List<AccountEntity>,
            walletName: String,
            createIfMissing: Boolean,
        ): Long? {
            val existing =
                accounts.firstOrNull {
                    it.type == AccountType.WALLET && it.name.equals(walletName, ignoreCase = true)
                }
            if (existing != null) return existing.id
            if (!createIfMissing) return null
            return accountDao.insert(
                AccountEntity(
                    name = walletName,
                    type = AccountType.WALLET,
                    smsAutoCreated = true,
                ),
            )
        }

        private fun findBankByLastFour(
            accounts: List<AccountEntity>,
            last4: String,
        ): Long? =
            accounts
                .firstOrNull {
                    (it.type == AccountType.BANK || it.type == AccountType.DEBIT) &&
                        it.cardLastFour == last4
                }?.id

        companion object {
            const val WALLET_PLUXEE_NAME = "Pluxee Meal"
            const val WALLET_HP_PAY_NAME = "HP Pay"
        }
    }
