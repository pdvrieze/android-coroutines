package nl.adaptivity.android.darwin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.net.URI;

import nl.adaptivity.android.darwinlib.R;

/**
 * Created by pdvrieze on 24/05/16.
 */
public class AuthenticatedWebClientFactory {

  private static class ShowAccountTask extends AsyncTask<AuthenticatedWebClientCallbacks, Void, Runnable> {

    private final Context mContext;
    private final URI mAuthBase;

    public ShowAccountTask(final Context context, URI authBase) {mContext = context;
      mAuthBase = authBase;
    }

    @Override
    protected Runnable doInBackground(final AuthenticatedWebClientCallbacks... params) {
      final AuthenticatedWebClientCallbacks callbacks = params[0];
      AccountManager                        am        = AccountManager.get(mContext);
      if (! hasAuthenticator(am)) {
        return new Runnable() {
          @Override
          public void run() {
            callbacks.showDownloadDialog();
          }
        };
      }

      Account currentAccount = getStoredAccount(mContext);
      if (!isAccountValid(am, currentAccount, mAuthBase)) {
        currentAccount=null;
      }
      final Intent selectAccountIntent = selectAccount(mContext, currentAccount, mAuthBase);
      return new Runnable() {
        @Override
        public void run() {
          callbacks.startSelectAccountActivity(selectAccountIntent);
        }
      };
    }

    @Override
    protected void onPostExecute(final Runnable runnable) {
      runnable.run();
    }
  }

  private static final String[] DEFAULT_AUTHBASE_ARRAY = {null };

  public interface AuthenticatedWebClientCallbacks {
    /**
     * The system should present a download dialog. Probably using
     */
    void showDownloadDialog();

    void startSelectAccountActivity(Intent selectAccount);
  }

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  @WorkerThread
  public static String getAuthToken(final Activity activity, final URI authBase, final Account account) {
    return AuthenticatedWebClientV14.getAuthToken(activity, authBase, account);
  }

  public static URI getAuthBase(final URI mBase) {
    if (mBase==null) { return null; }
    return mBase.resolve("/accounts/");
  }

  static SharedPreferences getSharedPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static Account getStoredAccount(Context context) {
    final SharedPreferences preferences = getSharedPreferences(context);
    final String accountName = preferences.getString(AuthenticatedWebClient.KEY_ACCOUNT_NAME, null);
    return accountName == null ? null : new Account(accountName, AuthenticatedWebClient.ACCOUNT_TYPE);
  }

  public static void setStoredAccount(Context context, Account account) {
    final SharedPreferences preferences = getSharedPreferences(context);
    if (account==null) {
      preferences.edit().remove(AuthenticatedWebClient.KEY_ACCOUNT_NAME).apply();
    } else {
      preferences.edit().putString(AuthenticatedWebClient.KEY_ACCOUNT_NAME, account.name).apply();
    }
  }

  @WorkerThread
  public static boolean isAccountValid(Context context, Account account, URI authBase) {
    return isAccountValid(AccountManager.get(context), account, authBase);
  }

  @WorkerThread
  public static boolean isAccountValid(final AccountManager am, final Account account, final URI authBase) {
    if (account==null) { return false; }
    try {
      AccountManagerFuture<Boolean> future = am.hasFeatures(account, accountFeatures(authBase), null, null );
      return future.getResult();
    } catch (OperationCanceledException | IOException e) {
      throw new RuntimeException(e);
    } catch (SecurityException|AuthenticatorException e) {
      return false;
    }
  }

  public static boolean hasAuthenticator(Context context) {
    return hasAuthenticator(AccountManager.get(context));
  }

  public static boolean hasAuthenticator(final AccountManager am) {
    for (AuthenticatorDescription descriptor : am.getAuthenticatorTypes()) {
      if (AuthenticatedWebClient.ACCOUNT_TYPE.equals(descriptor.type)) {
        return true;
      }
    }
    return false;
  }


  static String[] accountFeatures(URI authbase) {
    if (authbase==null) { return DEFAULT_AUTHBASE_ARRAY; }
    return new String[] { authbase.toASCIIString() };
  }

  public static Intent selectAccount(Context context, Account account, URI authbase) {
    AccountManager am = AccountManager.get(context);
    final Bundle options;
    if (authbase == null) { options = null; } else {
      options = new Bundle(1);
      options.putString(AuthenticatedWebClient.KEY_AUTH_BASE, authbase.toASCIIString());
    }

    return am.newChooseAccountIntent(account, null, new String[]{AuthenticatedWebClient.ACCOUNT_TYPE}, false, context.getString(R.string.descriptionOverrideText), null, null, options);
  }

  public static Account handleSelectAcountActivityResult(final Context context, int resultCode, Intent resultData) {
    if (resultCode == Activity.RESULT_OK) {
      String accountName = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      String accountType = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
      Account account = new Account(accountName, accountType);
      setStoredAccount(context, account);
      return account;
    } // else ignore
    return getStoredAccount(context); // Just return the stored account instead.
  }

  public static boolean handleInstallAuthenticatorActivityResult(final Context context, int resultCode, Intent resultData) {
    return resultCode==Activity.RESULT_OK;
  }

  @WorkerThread
  public static Account tryEnsureAccount(final Context context, URI authBase, AuthenticatedWebClientCallbacks ensureCallbacks) {
    {
      Account account = getStoredAccount(context); // Get the stored account, if we have one check that it is valid
      if (account != null) {
        if (isAccountValid(context, account, authBase)) { return account; }
        setStoredAccount(context, null);
      }
    }
    if (!hasAuthenticator(context)) {
      ensureCallbacks.showDownloadDialog();
      return null;
    }
    Intent selectAccount = selectAccount(context, null, authBase);
    ensureCallbacks.startSelectAccountActivity(selectAccount);

    return null;
  }

  @UiThread
  public static void showAccountSelection(Activity activity, AuthenticatedWebClientCallbacks callbacks, URI authBase) {
    new ShowAccountTask(activity, authBase).execute(callbacks);
    Account currentAccount = getStoredAccount(activity);
  }

  public static void doShowDownloadDialog(Activity activity, int requestCode) {
    DownloadDialog dialog = DownloadDialog.newInstance(requestCode);
    dialog.show(activity.getFragmentManager(), AuthenticatedWebClient.DOWNLOAD_DIALOG_TAG);
  }

  public static AuthenticatedWebClient newClient(Context context, Account account, URI authbase) {
    return new AuthenticatedWebClientV14(context, account, authbase);
  }


}
