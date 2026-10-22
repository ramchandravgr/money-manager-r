package com.mmr.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.repository.BackupRepositoryImpl
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Dispatchers

@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentedTest {

    private lateinit var database: MmrDatabase
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepositoryImpl(database.backupDao(), Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestore_replacesDatabaseWithExportedSnapshot() = runBlocking {
        val backupDao = database.backupDao()
        backupDao.insertCategories(
            listOf(
                CategoryEntity(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
            )
        )
        backupDao.insertAccounts(
            listOf(
                AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK, isDefault = true)
            )
        )
        backupDao.insertTransactions(
            listOf(
                TransactionEntity(
                    id = 10L,
                    dateTime = 1_700_000_000_000L,
                    amount = 1000.0,
                    type = TransactionType.INCOME,
                    categoryId = 1L,
                    accountId = 1L,
                    status = RecordStatus.APPROVED
                )
            )
        )

        val exportedJson = repository.exportBackupJson()
        backupDao.clearAllUserData()

        val emptySummary = repository.getLocalDataSummary()
        assertEquals(0, emptySummary.totalRecords)

        val restored = repository.importBackupJson(exportedJson)
        assertEquals(1, restored.categories)
        assertEquals(1, restored.accounts)
        assertEquals(1, restored.transactions)

        val afterSummary = repository.getLocalDataSummary()
        assertEquals(3, afterSummary.totalRecords)
        assertEquals(1000.0, backupDao.getAllTransactions().first().amount, 0.001)
    }
}
