package nl.adaptivity.android.darwin

import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Simple implementation of the cookie store needed for authentication.
 */
class MyCookieStore : CookieStore {
    private val lock: ReentrantLock

    private val cookieMap: MutableMap<String, MutableList<HttpCookie>>
    private val uriMap: MutableMap<URI, MutableList<HttpCookie>>

    init {
        lock = ReentrantLock()
        cookieMap = HashMap()
        uriMap = HashMap()
    }

    override fun add(uri: URI, cookie: HttpCookie?) {
        if (cookie == null) throw NullPointerException()
        if (cookie.maxAge == 0L) {
            return
        }
        lock.lock()
        try {
            val host = uri.host
            var list: MutableList<HttpCookie>? = cookieMap[host]
            if (list == null) {
                list = ArrayList()
                cookieMap.put(host, list)
            }
            removeSameCookieFromList(list, cookie)
            list.add(cookie)

            try {
                val cookieUri = getCookieUri(uri)
                list = uriMap[cookieUri]
                if (list == null) {
                    list = ArrayList()
                    uriMap.put(cookieUri, list)
                }
                removeSameCookieFromList(list, cookie)
                list.add(cookie)
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }


        } finally {
            lock.unlock()
        }
    }

    private fun removeSameCookieFromList(list: MutableList<HttpCookie>?, cookie: HttpCookie): Boolean {
        if (list == null) {
            return false
        }
        var removed = false
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val elem = iterator.next()
            if (cookie.name == elem.name &&
                    isEqual(elem.domain, cookie.domain) &&
                    isEqualOrNull(elem.portlist, cookie.portlist) &&
                    isEqual(elem.path, cookie.path)) {
                iterator.remove()
                removed = true
            }
        }
        return removed
    }

    private fun isEqual(val1: Any?, val2: Any?): Boolean {
        return if (val1 == null) val2 == null else val1 == val2
    }

    private fun isEqualOrNull(val1: Any?, val2: Any?): Boolean {
        return val1 == null || val2 == null || val1 == val2
    }

    @Throws(URISyntaxException::class)
    private fun getCookieUri(uri: URI): URI {
        return URI("http", uri.host, uri.path)
    }

    override fun get(uri: URI): List<HttpCookie> {
        lock.lock()
        try {
            val host = uri.host
            val rawList = cookieMap[host]
            if (rawList == null || rawList.isEmpty()) {
                return emptyList()
            }
            val uriIsSecure = "https" == uri.scheme

            val result = ArrayList<HttpCookie>()
            val it = rawList.iterator()
            while (it.hasNext()) {
                val candidate = it.next()
                if (candidate.hasExpired()) {
                    it.remove()
                }
                if (!candidate.secure || uriIsSecure) {
                    result.add(candidate)
                }
            }
            return result
        } finally {
            lock.unlock()
        }
    }

    override fun getCookies(): List<HttpCookie> {
        val result = ArrayList<HttpCookie>()
        lock.lock()
        try {
            for (list in cookieMap.values) {
                val it = list.iterator()
                while (it.hasNext()) {
                    val cookie = it.next()
                    if (cookie.hasExpired()) {
                        it.remove()
                    } else {
                        result.add(cookie)
                    }
                }
            }
        } finally {
            lock.unlock()
        }
        return result
    }

    override fun getURIs(): List<URI> {
        lock.lock()
        try {
            return ArrayList(uriMap.keys)
        } finally {
            lock.unlock()
        }
    }

    override fun remove(uri: URI, cookie: HttpCookie): Boolean {
        lock.lock()
        try {
            var result = false
            cookieMap[uri.host]?.let { list ->
                result = removeSameCookieFromList(list, cookie)
            }
            uriMap[uri]?.let { list ->
                result = removeSameCookieFromList(list, cookie) || result
            }
            return result
        } finally {
            lock.unlock()
        }
    }

    override fun removeAll(): Boolean {

        lock.lock()
        var result = false
        for (cookieList in cookieMap.values) {
            if (cookieList.size > 0) {
                result = true
                break
            }
        }
        try {
            cookieMap.clear()
            uriMap.clear()
        } finally {
            lock.unlock()
        }
        return result
    }
}
