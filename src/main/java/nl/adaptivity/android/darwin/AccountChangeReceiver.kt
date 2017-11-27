package nl.adaptivity.android.darwin

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.adaptivity.android.accountmanager.accountName
import nl.adaptivity.android.accountmanager.accountType
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory

class AccountChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action==AccountManager.ACTION_ACCOUNT_REMOVED) {
            val storedAccount = AuthenticatedWebClientFactory.getStoredAccount(context)
            if (storedAccount?.name == intent.accountName && storedAccount?.type == intent.accountType) {
                AuthenticatedWebClientFactory.setStoredAccount(context, null)
            }
        }
    }
}
