package com.rrparallelstudio.miapp.ui.ftue

enum class FtueBulletIcon {
    SummaryIncome,
    SummaryExpense,
    SummaryCarryOver,
    SummaryTotal,
    AddTransaction,
    ViewMode,
    Search,
    ApprovalsInbox,
    SwipeMonth,
    MainStats,
    MainBudgets,
    MainAssets,
    MainScheduled,
    MainSettings,
    Backup,
    DriveBackup,
    LanSync,
    WebManage,
    Configuration,
    Accounts,
    Categories,
}

data class FtueBullet(
    val text: String,
    val icon: FtueBulletIcon? = null,
)

data class FtueWelcomeStep(
    val id: String,
    val title: String,
    val body: String,
    val bullets: List<FtueBullet> = emptyList(),
)

object FtueWelcomeCatalog {
    val steps: List<FtueWelcomeStep> =
        listOf(
            FtueWelcomeStep(
                id = "welcome",
                title = "Welcome to Money Inbox",
                body =
                    "Track spending offline on your phone. " +
                        "Your ledger stays on-device unless you choose to back it up.",
            ),
            FtueWelcomeStep(
                id = "home",
                title = "Home ledger",
                body = "Your month at a glance.",
                bullets =
                    listOf(
                        FtueBullet("Swipe left or right to change month.", FtueBulletIcon.SwipeMonth),
                        FtueBullet(
                            "Tap the center menu for Daily, Weekly, Monthly, or Summary. Tap Ledger again while already on Transactions to jump back to Daily and this month.",
                            FtueBulletIcon.ViewMode,
                        ),
                        FtueBullet("Tap + to add a transaction.", FtueBulletIcon.AddTransaction),
                        FtueBullet("Search finds past transactions.", FtueBulletIcon.Search),
                        FtueBullet(
                            "The mail icon opens Approval Center when you have pending items.",
                            FtueBulletIcon.ApprovalsInbox,
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "summary",
                title = "Period summary",
                body = "Icons above the bottom tabs explain this month. Tap any icon for a short explanation.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Income — money received in this period only.",
                            FtueBulletIcon.SummaryIncome,
                        ),
                        FtueBullet(
                            "Expenses — money spent in this period only.",
                            FtueBulletIcon.SummaryExpense,
                        ),
                        FtueBullet(
                            "Carry-over — balance from before (column appears only when not zero).",
                            FtueBulletIcon.SummaryCarryOver,
                        ),
                        FtueBullet(
                            "Total — period net; includes carry-over when that column is shown.",
                            FtueBulletIcon.SummaryTotal,
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "main_tabs",
                title = "Bottom navigation",
                body = "Open these from the tab bar. Hide tabs you do not need in Settings → Transaction Appearance.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Trends — charts by category. Slide Income or Expenses; totals stay on the tabs.",
                            FtueBulletIcon.MainStats,
                        ),
                        FtueBullet(
                            "Budgets — monthly limits and progress.",
                            FtueBulletIcon.MainBudgets,
                        ),
                        FtueBullet(
                            "Assets — track valuables, valuations, and reminders.",
                            FtueBulletIcon.MainAssets,
                        ),
                        FtueBullet(
                            "Scheduled — loans, EMIs, and recurring transactions.",
                            FtueBulletIcon.MainScheduled,
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "approvals",
                title = "Approval Center",
                body =
                    "Paste a bank SMS, review the parsed amount and category, then approve before " +
                        "it joins your ledger. You can edit, link to an existing transaction, or reject.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Open from the mail icon on Home.",
                            FtueBulletIcon.ApprovalsInbox,
                        ),
                        FtueBullet(
                            "Refresh — import new bank SMS from your backup folder (set under Settings → Transaction scan).",
                            FtueBulletIcon.ApprovalsInbox,
                        ),
                        FtueBullet(
                            "If you change the account, later SMS with the same pattern use that account (included in backup and Peer Device Sync).",
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "settings",
                title = "Settings worth knowing",
                body = "Everything in Settings is optional tuning — defaults work for a quick start.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Transaction Appearance — theme, colors, start screen, visible tabs.",
                            FtueBulletIcon.Configuration,
                        ),
                        FtueBullet(
                            "Accounts — cash, bank, cards; pick one when you add a transaction.",
                            FtueBulletIcon.Accounts,
                        ),
                        FtueBullet(
                            "Categories — organize income and spending.",
                            FtueBulletIcon.Categories,
                        ),
                        FtueBullet(
                            "Transaction scan — SMS backup folder and how far back to read bank SMS.",
                            FtueBulletIcon.ApprovalsInbox,
                        ),
                        FtueBullet(
                            "Replay introduction — run this tour again anytime.",
                            FtueBulletIcon.MainSettings,
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "backup",
                title = "Backup & sync",
                body = "No cloud account is required. Export when you want a copy off this phone.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Backup & Restore — export or import a signed .mibxbac file.",
                            FtueBulletIcon.Backup,
                        ),
                        FtueBullet(
                            "Google Drive backup — optional scheduled uploads.",
                            FtueBulletIcon.DriveBackup,
                        ),
                        FtueBullet(
                            "Peer Device Sync — merge on the same Wi‑Fi or your hotspot. Auto sync when you open the app, or tap Sync now. Theme stays on each phone.",
                            FtueBulletIcon.LanSync,
                        ),
                        FtueBullet(
                            "Web Manage — edit the ledger from a browser on your Wi‑Fi or phone hotspot.",
                            FtueBulletIcon.WebManage,
                        ),
                    ),
            ),
        )
}
