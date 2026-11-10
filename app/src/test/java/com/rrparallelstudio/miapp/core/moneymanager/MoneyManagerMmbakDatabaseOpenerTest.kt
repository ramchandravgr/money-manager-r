package com.rrparallelstudio.miapp.core.moneymanager

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MoneyManagerMmbakDatabaseOpenerTest {
    private lateinit var dbFile: File
    private lateinit var database: SQLiteDatabase

    @Before
    fun setUp() {
        dbFile = File.createTempFile("mmbak-test", ".db")
        database = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        database.execSQL(
            """
            CREATE TABLE INOUTCOME (
                UID INTEGER PRIMARY KEY,
                WDATE INTEGER NOT NULL,
                ZMONEY REAL NOT NULL,
                DO_TYPE INTEGER NOT NULL,
                ASSETUID INTEGER,
                CTGUID INTEGER,
                ZCONTENT TEXT,
                IS_DEL INTEGER DEFAULT 0
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE ASSETS (
                UID INTEGER PRIMARY KEY,
                NIC_NAME TEXT NOT NULL,
                IS_DEL INTEGER DEFAULT 0
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE ZCATEGORY (
                UID INTEGER PRIMARY KEY,
                NAME TEXT NOT NULL,
                TYPE INTEGER,
                PUID INTEGER,
                IS_DEL INTEGER DEFAULT 0
            )
            """.trimIndent(),
        )
        database.execSQL("INSERT INTO ASSETS (UID, NIC_NAME) VALUES (1, 'Cash')")
        database.execSQL(
            """
            INSERT INTO INOUTCOME (UID, WDATE, ZMONEY, DO_TYPE, ASSETUID, CTGUID, ZCONTENT)
            VALUES (100, 1700000000000, 250.5, 1, 1, 10, 'Lunch')
            """.trimIndent(),
        )
        database.execSQL(
            "INSERT INTO ZCATEGORY (UID, NAME, TYPE, PUID) VALUES (10, 'Food', 1, 0)",
        )
    }

    @After
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun readTransactions_parsesAndroidSchema() {
        val rows = MoneyManagerMmbakDatabaseOpener.readTransactions(database)
        assertEquals(1, rows.size)
        assertEquals(100L, rows[0].uid)
        assertEquals(1, rows[0].doType)
        assertEquals(250.5, rows[0].amount, 0.01)
        assertEquals("Lunch", rows[0].note)
    }

    @Test
    fun readAssetNames_resolvesNickname() {
        val names = MoneyManagerMmbakDatabaseOpener.readAssetNames(database)
        assertEquals("Cash", names[1L])
    }
}
