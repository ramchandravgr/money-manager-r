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
                body = "Open these from the tab bar. Hide tabs you do not need in Settings → Home & Menu.",
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
                            "Refresh — import new bank SMS from your backup folder (set under Settings → Transaction Scan).",
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
                            "Home & Menu — start screen, month and week, visible tabs.",
                            FtueBulletIcon.Configuration,
                        ),
                        FtueBullet(
                            "Appearance — theme, light or dark, and transaction list style.",
                            FtueBulletIcon.Configuration,
                        ),
                        FtueBullet(
                            "Security — lock the app when you open it.",
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
                            "Transaction Scan — SMS backup folder, how far back to read, and optional custom SMS templates.",
                            FtueBulletIcon.ApprovalsInbox,
                        ),
                        FtueBullet(
                            "Replay Introduction — run this tour again anytime.",
                            FtueBulletIcon.MainSettings,
                        ),
                    ),
            ),
            FtueWelcomeStep(
                id = "backup",
                title = "Backup & Sync",
                body = "No cloud account is required. Back up to a folder you choose on this phone.",
                bullets =
                    listOf(
                        FtueBullet(
                            "Backup & Restore — Back Up Now saves a signed .mibxbac to your backup folder; " +
                                "one schedule covers local files and Google Drive when linked. Import merges Money Manager or Monefy backups.",
                            FtueBulletIcon.Backup,
                        ),
                        FtueBullet(
                            "Google Drive backup — link your account; the same local file uploads (Wi‑Fi only optional).",
                            FtueBulletIcon.DriveBackup,
                        ),
                        FtueBullet(
                            "Ledger spreadsheet — export periods from Home as Excel or XML; import via template on Backup & Restore.",
                            FtueBulletIcon.Backup,
                        ),
                        FtueBullet(
                            "Settings → Peer Device Sync — turn on to merge with another phone on the same " +
                                "Wi‑Fi or phone hotspot. An ongoing notification may show while sync stays on; " +
                                "themes stay separate on each phone.",
                            FtueBulletIcon.LanSync,
                        ),
                        FtueBullet(
                            "Pairing — the host shows a code; the other phone enters it and you tap Approve " +
                                "on the host. On a hotspot, pick On this network or enter the other phone's " +
                                "address on Sync now if discovery fails.",
                            FtueBulletIcon.LanSync,
                        ),
                        FtueBullet(
                            "Settings → Web Manage — set a password, turn it on, then open the link in a browser " +
                                "on the same Wi‑Fi or hotspot.",
                            FtueBulletIcon.WebManage,
                        ),
                        FtueBullet(
                            "Only one of Peer Device Sync or Web Manage can listen on the LAN at a time. " +
                                "Turn the other off first if a toggle is blocked.",
                            FtueBulletIcon.WebManage,
                        ),
                    ),
            ),
        )
}
