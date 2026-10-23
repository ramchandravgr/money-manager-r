package com.mmr.core.parser.account

import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.SmsInstrument
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAccountResolver
    @Inject
    constructor(
        private val accountDao: AccountDao,
    ) {
        suspend fun resolveAccountId(candidate: ParsedSmsCandidate): Long? {
            val accounts = accountDao.getActiveAccountsOnce()
            return when {
                SmsSpendChannel.isCreditCardSpend(candidate) -> {
                    val last4 = candidate.accountLastDigits ?: return null
                    findOrCreateCreditCard(accounts, last4)
                }
                SmsSpendChannel.isPluxeeMealSpend(candidate) ->
                    findOrCreateWallet(accounts, WALLET_PLUXEE_NAME)
                SmsSpendChannel.isHpPayWalletSpend(candidate) ->
                    findOrCreateWallet(accounts, WALLET_HP_PAY_NAME)
                candidate.instrument == SmsInstrument.SAVINGS && !candidate.accountLastDigits.isNullOrBlank() -> {
                    findBankByLastFour(accounts, candidate.accountLastDigits!!)
                }
                else -> null
            }
        }

        private suspend fun findOrCreateCreditCard(
            accounts: List<AccountEntity>,
            last4: String,
        ): Long {
            val existing =
                accounts.firstOrNull {
                    it.type == AccountType.CREDIT && it.cardLastFour == last4
                } ?: accounts.firstOrNull {
                    it.type == AccountType.CREDIT && it.name.contains(last4)
                }
            if (existing != null) return existing.id
            val name = "Credit card • $last4"
            return accountDao.insert(
                AccountEntity(
                    name = name,
                    type = AccountType.CREDIT,
                    cardLastFour = last4,
                ),
            )
        }

        private suspend fun findOrCreateWallet(
            accounts: List<AccountEntity>,
            walletName: String,
        ): Long {
            val existing =
                accounts.firstOrNull {
                    it.type == AccountType.WALLET && it.name.equals(walletName, ignoreCase = true)
                }
            if (existing != null) return existing.id
            return accountDao.insert(
                AccountEntity(
                    name = walletName,
                    type = AccountType.WALLET,
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
