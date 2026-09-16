// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.base

import android.content.res.AssetManager
import android.os.Build
import com.osfans.trime.util.FileUtils
import com.osfans.trime.util.ResourceUtils
import com.osfans.trime.util.appContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Resolve [name] under [parent].
 *
 * Returns null when [parent] is null, cannot be created, or the resulting directory
 * is not writable. Callers must not cache a failed result permanently; retry when
 * storage may have become available (for example after reboot).
 */
internal fun resolveWritableChildDir(
    parent: File?,
    name: String,
): File? {
    if (parent == null) return null
    if (!parent.exists() && !parent.mkdirs()) return null
    if (!parent.canWrite()) return null
    val dir = File(parent, name)
    if (!dir.exists() && !dir.mkdirs()) return null
    return dir.takeIf { it.canWrite() }
}

object DataManager {
    const val DEFAULT_CUSTOM_FILE_NAME = "default.custom.yaml"
    const val USER_CONFIG_FILE_NAME = "user.yaml"
    const val INSTALLATION_FILE_NAME = "installation.yaml"

    val POST_SCHEMA_DEPLOY_EXPORT_FILES =
        listOf(
            DEFAULT_CUSTOM_FILE_NAME,
            USER_CONFIG_FILE_NAME,
        )

    private const val DATA_CHECKSUMS_NAME = "checksums.json"
    private const val SHARED_DIR_NAME = "shared"
    private const val USER_DIR_NAME = "rime"

    private const val SCHEMA_LIST_CUSTOM_PATCH = """
      patch:
        schema_list:
          - schema: luna_pinyin
          - schema: luna_pinyin_simp
    """

    private val lock = ReentrantLock()

    private val json by lazy { Json }

    private fun deserializeDataChecksums(raw: String): DataChecksums = json.decodeFromString<DataChecksums>(raw)

    // If Android version supports direct boot, we put the hierarchy in device encrypted storage
    // instead of credential encrypted storage so that data can be accessed before user unlock
    private val dataDir: File =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Timber.d("Using device protected storage")
            appContext.createDeviceProtectedStorageContext().dataDir
        } else {
            File(appContext.applicationInfo.dataDir)
        }

    private fun AssetManager.dataChecksums(): DataChecksums = open(DATA_CHECKSUMS_NAME)
        .bufferedReader()
        .use { it.readText() }
        .let { deserializeDataChecksums(it) }

    @Volatile
    private var cachedSharedDataDir: File? = null

    @Volatile
    private var cachedUserDataDir: File? = null

    private fun resolveAppScopedDir(
        cached: File?,
        name: String,
        store: (File) -> Unit,
    ): File? {
        cached?.takeIf { it.canWrite() }?.let { return it }
        val resolved =
            resolveWritableChildDir(appContext.getExternalFilesDir(null), name) ?: return null
        store(resolved)
        return resolved
    }

    /** Writable shared assets dir, or null when external app files are not ready yet. */
    fun resolvedSharedDataDir(): File? = resolveAppScopedDir(cachedSharedDataDir, SHARED_DIR_NAME) { cachedSharedDataDir = it }

    /** Writable Rime user dir, or null when external app files are not ready yet. */
    fun resolvedUserDataDir(): File? = resolveAppScopedDir(cachedUserDataDir, USER_DIR_NAME) { cachedUserDataDir = it }

    val sharedDataDir: File
        get() =
            resolvedSharedDataDir()
                ?: error("Shared data dir is not available")

    /** App-scoped path used by Rime at runtime. */
    val userDataDir: File
        get() =
            resolvedUserDataDir()
                ?: error("User data dir is not available")

    val prebuiltDataDir: File
        get() = File(sharedDataDir, "build")
    val stagingDir get() = File(userDataDir, "build")

    /**
     * Return the absolute path of the compiled config file
     * based on given resource id.
     *
     * @param resourceId usually equals the config file name without the extension
     * @return the absolute path of the compiled config file
     */
    @JvmStatic
    fun resolveDeployedResourcePath(resourceId: String): String {
        val defaultPath = File(stagingDir, "$resourceId.yaml")
        if (!defaultPath.exists()) {
            val fallbackPath = File(prebuiltDataDir, "$resourceId.yaml")
            if (fallbackPath.exists()) return fallbackPath.absolutePath
        }
        return defaultPath.absolutePath
    }

    fun sync() = lock.withLock {
        val oldChecksumsFile = File(dataDir, DATA_CHECKSUMS_NAME)
        val oldChecksums =
            oldChecksumsFile
                .runCatching { deserializeDataChecksums(bufferedReader().use { it.readText() }) }
                .getOrElse { DataChecksums("", emptyMap()) }

        val newChecksums = appContext.assets.dataChecksums()

        DataDiff.diff(oldChecksums, newChecksums).sortedByDescending { it.ordinal }.forEach {
            Timber.d("Diff: $it")
            when (it) {
                is DataDiff.CreateFile,
                is DataDiff.UpdateFile,
                -> {
                    val destPath = sharedDataDir.resolveSibling(it.path).absolutePath
                    ResourceUtils.copyFile(it.path, destPath)
                }

                is DataDiff.DeleteDir,
                is DataDiff.DeleteFile,
                -> FileUtils.delete(sharedDataDir.resolve(it.path.substringAfterLast('/'))).getOrThrow()
            }
        }

        ResourceUtils.copyFile(DATA_CHECKSUMS_NAME, dataDir.resolve(DATA_CHECKSUMS_NAME).absolutePath)

        val custom = userDataDir.resolve(DEFAULT_CUSTOM_FILE_NAME)
        if (!custom.exists()) {
            if (custom.createNewFile()) {
                custom.writeText(SCHEMA_LIST_CUSTOM_PATCH.trimIndent())
            }
        }

        Timber.d("Synced!")
    }
}
