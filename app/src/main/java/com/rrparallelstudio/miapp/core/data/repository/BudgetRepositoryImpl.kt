package com.rrparallelstudio.miapp.core.data.repository

import com.rrparallelstudio.miapp.budget.BudgetAlertNotifier
import com.rrparallelstudio.miapp.core.budget.BudgetPeriodBounds
import com.rrparallelstudio.miapp.core.budget.BudgetPeriodResolver
import com.rrparallelstudio.miapp.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.database.dao.BudgetDao
import com.rrparallelstudio.miapp.core.database.dao.CategoryDao
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.domain.model.BudgetProgressSnapshot
import com.rrparallelstudio.miapp.core.domain.repository.BudgetRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneRecorder
import com.rrparallelstudio.miapp.core.lansync.SyncEntityType
import com.rrparallelstudio.miapp.core.owner.withLocalCreatedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl
    @Inject
    constructor(
        private val budgetDao: BudgetDao,
        private val categoryDao: CategoryDao,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val budgetAlertNotifier: BudgetAlertNotifier,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
        private val ownerProfileCoordinator: com.rrparallelstudio.miapp.core.owner.MmrOwnerProfileCoordinator,
    ) : BudgetRepository {
        override fun observeActiveBudgetProgress(): Flow<List<BudgetProgressSnapshot>> =
            combine(
                budgetDao.observeActive(),
                displaySettingsStore.settings,
            ) { budgets, display ->
                budgets.mapNotNull { budget -> progressSnapshot(budget, display) }
            }

        override suspend fun getById(id: Long): BudgetEntity? = budgetDao.getById(id)

        override suspend fun upsert(entity: BudgetEntity): Long {
            val stamped =
                if (entity.id > 0L) {
                    entity
                } else {
                    entity.withLocalCreatedBy(ownerProfileCoordinator.localStamp())
                }
            val id =
                if (stamped.id > 0L) {
                    budgetDao.update(stamped)
                    stamped.id
                } else {
                    budgetDao.insert(stamped)
                }
            widgetRefreshTrigger.requestRefresh()
            return id
        }

        override suspend fun delete(id: Long) {
            val existing = budgetDao.getById(id) ?: return
            tombstoneRecorder.recordDeletion(existing.syncId, SyncEntityType.BUDGET)
            budgetDao.delete(existing)
            widgetRefreshTrigger.requestRefresh()
        }

        override suspend fun evaluateAlertsForTransaction(
            categoryId: Long?,
            subcategoryId: Long?,
            transactionMillis: Long,
        ) {
            if (categoryId == null || categoryId <= 0L) return
            val display = displaySettingsStore.snapshot()
            for (budget in budgetDao.getActiveOnce()) {
                if (!budget.alertOnExceeded) continue
                if (!matchesTarget(budget, categoryId, subcategoryId)) continue
                val bounds =
                    BudgetPeriodResolver.resolve(
                        budget = budget,
                        anchorMillis = transactionMillis,
                        monthStartDay = display.monthStartDay,
                        weekStartDay = display.weekStartDay,
                    )
                if (transactionMillis !in bounds.startMillis..bounds.endMillis) continue
                val spent = sumSpent(budget, bounds.startMillis, bounds.endMillis)
                if (spent <= budget.amountLimit) continue
                if (budget.lastAlertPeriodKey == bounds.periodKey) continue
                val snapshot = progressSnapshot(budget, display, spent, bounds) ?: continue
                budgetAlertNotifier.notifyExceeded(snapshot)
                budgetDao.update(
                    budget.copy(
                        lastAlertPeriodKey = bounds.periodKey,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        private suspend fun progressSnapshot(
            budget: BudgetEntity,
            display: AppDisplaySettingsSnapshot,
            spentOverride: Double? = null,
            boundsOverride: BudgetPeriodBounds? = null,
        ): BudgetProgressSnapshot? {
            val category = categoryDao.getCategoryById(budget.categoryId) ?: return null
            val sub =
                budget.subcategoryId?.let { id ->
                    categoryDao.getSubcategoryById(id)
                }
            val bounds =
                boundsOverride
                    ?: BudgetPeriodResolver.resolve(
                        budget = budget,
                        monthStartDay = display.monthStartDay,
                        weekStartDay = display.weekStartDay,
                    )
            val spent =
                spentOverride
                    ?: sumSpent(budget, bounds.startMillis, bounds.endMillis)
            val targetLabel =
                if (sub != null) {
                    "${category.name} · ${sub.name}"
                } else {
                    category.name
                }
            return BudgetProgressSnapshot(
                budgetId = budget.id,
                categoryId = budget.categoryId,
                subcategoryId = budget.subcategoryId,
                targetLabel = targetLabel,
                periodLabel = bounds.label,
                periodType = budget.periodType,
                limitAmount = budget.amountLimit,
                spentAmount = spent,
                alertOnExceeded = budget.alertOnExceeded,
            )
        }

        private suspend fun sumSpent(
            budget: BudgetEntity,
            startMillis: Long,
            endMillis: Long,
        ): Double =
            if (budget.subcategoryId != null) {
                budgetDao.sumExpenseForSubcategory(
                    categoryId = budget.categoryId,
                    subcategoryId = budget.subcategoryId,
                    startMillis = startMillis,
                    endMillis = endMillis,
                )
            } else {
                budgetDao.sumExpenseForCategory(
                    categoryId = budget.categoryId,
                    startMillis = startMillis,
                    endMillis = endMillis,
                )
            }

        private fun matchesTarget(
            budget: BudgetEntity,
            categoryId: Long,
            subcategoryId: Long?,
        ): Boolean {
            if (budget.categoryId != categoryId) return false
            return budget.subcategoryId == null || budget.subcategoryId == subcategoryId
        }
    }
