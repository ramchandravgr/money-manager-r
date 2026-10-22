package com.mmr.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryInstrumentedTest {

    private lateinit var database: MmrDatabase
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTransaction_requiresExistingAccount() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.insertTransaction(
                        TransactionEntity(
                            dateTime = System.currentTimeMillis(),
                            amount = 50.0,
                            type = TransactionType.EXPENSE,
                            accountId = 99L,
                            status = RecordStatus.APPROVED
                        )
                    )
                }
            }
        }
    }

    @Test
    fun insertTransaction_succeedsForValidPayload() = runBlocking {
        database.accountDao().insert(AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true))
        val accountId = database.accountDao().getAccounts().first().first().id

        val id = repository.insertTransaction(
            TransactionEntity(
                dateTime = System.currentTimeMillis(),
                amount = 75.0,
                type = TransactionType.EXPENSE,
                accountId = accountId,
                status = RecordStatus.APPROVED
            )
        )
        assertTrue(id > 0L)
    }
}
