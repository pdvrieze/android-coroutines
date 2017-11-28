package nl.adaptivity.android.darwin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.net.HttpURLConnection;

/**
 * Callback interface for handling request success.
 */

public interface IRequestFailure extends Serializable{
    void onFailure(@Nullable HttpURLConnection connection);
}
