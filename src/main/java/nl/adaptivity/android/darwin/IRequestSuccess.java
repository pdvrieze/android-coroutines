package nl.adaptivity.android.darwin;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.net.HttpURLConnection;

/**
 * Callback interface for handling request success.
 */

public interface IRequestSuccess extends Serializable{
    void onSuccess(@NotNull HttpURLConnection connection);
}
