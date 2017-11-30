# Warning
* This library works, but it's API is not yet stable. It was developed as proof of concept, the best API (esp names) was not
a core consideration. *

# android-coroutines
While Android is powerful it's activity model suffers from callback hell. Kotlin coroutines are supposed to fix this, but
Android is special. Your code can be kicked out of memory at any moment so serialization interferes with coroutines. 
Getting events back to the coroutine depends on your activity (actually we can use fragments instead - fragments can
be difficult, but great here)

#Currently supported functionality
##startActivityForResult
In your activity you can use a coroutine to just get the result of invoking another one:

```kotlin
fun onButtonClick(v:View) {
    launch {
       activityResult(Intent(MyDelegateActivity::class.java)).onOk { resultIntent ->
          runOnUiThread { textView.text = newText }
       }
    }
}
```

There are some variations of this, and the `Maybe` implementation used has many options.

##`SuspendableDialog`
In many cases you have a dialog to get some input from the user. From yes-no questions to input of values. SuspendableDialog is
a subclass of `DialogFragment` that provides the building blocks to have a dialog that is used in a coroutine. The actual dialog
implementation just has to invoke `dispatchResult` with the appropriate result value and the dialog is handled. Dismissal or
cancellation are handled by default.

## DownloadManager
**Warning - not quite complete **
Using the download manager is not quite straightforward even though you get a lot for free. The `DownloadFragment.download(Activity, Uri)` 
function will download your file and resume your coroutine when complette.

### TODO(DownloadManager)
- Handle download completion when the activity/fragment is not visible (on resume check the status and invoke the continuation
  as appropriate)
  
## AccountManager
AccountManager is not pretty, but this class makes it a bit prettier. In particular it implements a suspending wrapper
around getAuthToken that will even invoke a permission intent if needed (as was possible before Kitkat, but will no longer work).
