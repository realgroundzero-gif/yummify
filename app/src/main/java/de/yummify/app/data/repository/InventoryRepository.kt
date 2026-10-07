package de.yummify.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.yummify.app.data.model.InventoryItem
import de.yummify.app.data.model.InventoryChoices
import de.yummify.app.data.model.InventoryMath
import de.yummify.app.data.model.compactProductNotes
import de.yummify.app.data.model.normalized
import de.yummify.app.data.remote.NotionInventoryApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Tombstones remain in the durable cache until the remote archive succeeds.
data class InventorySyncState(val busy: Boolean = false, val configured: Boolean = false,
    val pending: Int = 0, val message: String = "Lokal gespeichert · Notion noch nicht eingerichtet")

class InventoryRepository internal constructor(context: Context, private val apiFactory: (String) -> NotionInventoryApi = { NotionInventoryApi(it) }) {
    private val prefs = context.getSharedPreferences("yummify_inventory", Context.MODE_PRIVATE)
    private val configRepo = UserPreferencesRepository.getInstance(context)
    private val gson = Gson()
    private val mutex = Mutex()
    private val syncMutex = Mutex()
    private val syncAgain = java.util.concurrent.atomic.AtomicBoolean(false)
    private var loadWarning: String? = null
    private val obsoleteImages = mutableListOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var database = configRepo.preferences.value.inventoryDatabaseIdInput
    private var records = load(database)
    private var purchases = prefs.getStringSet("purchases_${database}", emptySet()).orEmpty().toSet()
    private val _items = MutableStateFlow(records.filterNot { it.deleted })
    val items = _items.asStateFlow()
    private fun loadChoices(id: String) = prefs.getString("choices_$id", null)?.let { gson.fromJson(it, InventoryChoices::class.java) } ?: InventoryChoices()
    private val _choices = MutableStateFlow(loadChoices(database))
    val choices = _choices.asStateFlow()
    private fun persistChoices(value: InventoryChoices) {
        check(prefs.edit().putString("choices_$database", gson.toJson(value)).commit()) { "Auswahl konnte nicht gespeichert werden." }
        _choices.value = value
    }
    suspend fun refreshChoices(itemId: String? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val config = configRepo.preferences.value
            if (config.tokenInput.isBlank() || database.isBlank()) return@withLock
            val api = apiFactory(config.tokenInput)
            persistChoices(api.choices(api.source(database)))
            val item = records.firstOrNull { it.id == itemId && !it.coverPending && it.pageId != null }
            if (item != null) {
                val cover = api.readCover(item.pageId!!)
                records = records.map { if (it.id == item.id) it.copy(coverUrl = cover) else it }
                persist()
            }
        }
    }
    private val _syncState = MutableStateFlow(InventorySyncState(pending = records.count { it.dirty }).let { state ->
        loadWarning?.let { state.copy(message = it) } ?: state
    })
    val syncState = _syncState.asStateFlow()
    init {
        scope.launch {
            configRepo.preferences.map { Triple(it.tokenInput, it.inventoryDatabaseIdInput, it.autoSyncEnabled) }.distinctUntilChanged().collect { (_, id, auto) ->
                mutex.withLock {
                    if (database != id) {
                        val local = if (database.isBlank() && id.isNotBlank()) records else emptyList()
                        val existing = load(id)
                        database = id
                        _choices.value = loadChoices(id)
                        purchases = prefs.getStringSet("purchases_${database}", emptySet()).orEmpty().toSet()
                        records = existing + local.filter { item -> existing.none { it.id == item.id } }
                        persist()
                        if (local.isNotEmpty()) prefs.edit().remove(key("")).commit()
                    }
                    updateStatus()
                }
                if (auto) sync()
            }
        }
        scope.launch {
            while (isActive) {
                delay(60_000)
                if (configRepo.preferences.value.autoSyncEnabled && syncState.value.pending > 0) sync()
            }
        }
    }
    private fun key(id: String) = "items_${id.ifBlank { "local" }}"
    private fun load(id: String): List<InventoryItem> {
        val raw = prefs.getString(key(id), null) ?: return emptyList()
        val stored: List<InventoryItem> = try {
            gson.fromJson<List<InventoryItem>>(raw, object : TypeToken<List<InventoryItem>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            // Keep the unreadable data under a backup key instead of crashing on every start or overwriting it.
            prefs.edit().putString("${key(id)}_corrupt_${System.currentTimeMillis()}", raw).remove(key(id)).commit()
            loadWarning = "Gespeichertes Inventar war beschädigt und wurde gesichert. Ein Abgleich lädt den Stand aus Notion."
            return emptyList()
        }
        val compact = stored.map { it.normalized().compactProductNotes() }
        if (compact != stored) check(prefs.edit().putString(key(id), gson.toJson(compact)).commit()) { "Produktnotizen konnten nicht gespeichert werden." }
        return compact
    }
    private fun persist() {
        check(prefs.edit().putString(key(database), gson.toJson(records)).putStringSet("purchases_${database}", purchases).commit()) { "Inventar konnte nicht gespeichert werden." }
        _items.value = records.filterNot { it.deleted }
        updateStatus()
    }
    private fun updateStatus() {
        val config = configRepo.preferences.value
        val configured = config.tokenInput.isNotBlank() && database.isNotBlank()
        _syncState.value = _syncState.value.copy(configured = configured, pending = records.count { it.dirty },
            message = if (!configured) "Lokal gespeichert · Notion noch nicht eingerichtet" else if (_syncState.value.message.contains("noch nicht eingerichtet")) "Notion eingerichtet · Bereit zum Synchronisieren" else _syncState.value.message)
    }
    private suspend fun mutate(action: () -> Unit) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val before = records
            val previousPurchases = purchases
            try { action(); persist() }
            catch (e: Exception) { records = before; purchases = previousPurchases; throw e }
        }
        if (configRepo.preferences.value.autoSyncEnabled) scope.launch { sync() }
    }
    suspend fun save(item: InventoryItem) {
        require(item.validate() == null) { item.validate().orEmpty() }
        mutate {
            require(item.barcode.isBlank() || records.none { !it.deleted && it.id != item.id && it.barcode.trim() == item.barcode.trim() } || records.any { it.id == item.id }) {
                "Dieser Barcode ist bereits im Inventar. Bitte den vorhandenen Artikel öffnen."
            }
            records.firstOrNull { it.id == item.id }?.localCoverPath?.takeIf { it != item.localCoverPath }?.let { synchronized(obsoleteImages) { obsoleteImages += it } }
            records = records.filterNot { it.id == item.id } + item.copy(dirty = true)
        }
        deleteObsoleteImages()
    }
    suspend fun adjust(id: String, delta: Double) = mutate {
        records = records.map { if (it.id == id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(0.0), dirty = true) else it }
    }
    suspend fun remove(id: String) = mutate {
        records = records.map { if (it.id == id) it.copy(deleted = true, dirty = true) else it }
    }
    /** Photos are private copies; delete them once no record refers to them anymore. */
    private fun deleteObsoleteImages() {
        val paths = synchronized(obsoleteImages) { obsoleteImages.toList().also { obsoleteImages.clear() } }
        paths.filter { path -> records.none { it.localCoverPath == path } }.forEach { java.io.File(it).delete() }
    }
    suspend fun consume(ingredients: List<de.yummify.app.data.model.Ingredient>, multiplier: Double) = mutate {
        records = InventoryMath.consume(ingredients, multiplier, records)
    }
    suspend fun addPurchased(item: InventoryItem, purchaseId: String) = mutate {
        require(item.validate() == null) { item.validate().orEmpty() }
        if (purchaseId in purchases) return@mutate
        val existing = records.firstOrNull { !it.deleted && InventoryMath.normalizedName(it.name) == InventoryMath.normalizedName(item.name) && it.unit == item.unit && it.expiry == item.expiry && it.location == item.location }
        records = if (existing == null) records + item else records.map { if (it.id == existing.id) it.copy(quantity = it.quantity + item.quantity, dirty = true) else it }
        purchases = purchases + purchaseId
    }
    /**
     * Network calls run outside [mutex], so edits are never blocked by a slow Notion. A record that
     * changes while its upload is running stays dirty and is sent again in the next round.
     */
    suspend fun sync(): Unit = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) { syncAgain.set(true); return@withContext }
        try {
            do {
                syncAgain.set(false)
                syncOnce()
            } while (syncAgain.get())
        } finally { syncMutex.unlock() }
    }

    private suspend fun syncOnce() {
        val (token, db) = mutex.withLock {
            val config = configRepo.preferences.value
            if (config.tokenInput.isBlank() || database.isBlank()) { updateStatus(); return }
            config.tokenInput to database
        }
        _syncState.value = _syncState.value.copy(busy = true, configured = true, message = "Synchronisiert …")
        try {
            val api = apiFactory(token)
            val source = api.source(db)
            val remoteChoices = api.choices(source)
            val remote = api.all(source)
            require(remote.map { it.id }.distinct().size == remote.size) { "Doppelte Artikel-IDs in Notion. Bitte die IDs bereinigen." }
            val pending = mutex.withLock {
                if (database != db) return
                persistChoices(remoteChoices)
                // Dirty local entries take precedence. Unchanged remote removals are reflected locally.
                val dirty = records.filter { it.dirty }
                records = remote.filter { row -> dirty.none { it.id == row.id || (it.pageId != null && it.pageId == row.pageId) } }
                    .map { row -> row.copy(localCoverPath = records.firstOrNull { it.id == row.id || (row.pageId != null && it.pageId == row.pageId) }?.localCoverPath).compactProductNotes() } + dirty
                persist()
                records.filter { it.dirty }
            }
            pending.forEach { snapshot ->
                val remoteItem = remote.firstOrNull { it.id == snapshot.id || (snapshot.pageId != null && it.pageId == snapshot.pageId) }
                val item = snapshot.copy(pageId = snapshot.pageId ?: remoteItem?.pageId)
                if (item.deleted) {
                    api.delete(source, item)
                    mutex.withLock {
                        if (database != db) return
                        item.localCoverPath?.let { synchronized(obsoleteImages) { obsoleteImages += it } }
                        records = records.filterNot { it.id == item.id && it.deleted }
                        persist()
                        deleteObsoleteImages()
                    }
                } else {
                    val pageId = api.save(source, item)
                    val savedCover = api.savedCover(pageId)
                    mutex.withLock {
                        if (database != db) return
                        records = records.map { current -> if (current.id == item.id) applySaved(current, snapshot, item, pageId, savedCover) else current }
                        persist()
                    }
                }
            }
            _syncState.value = _syncState.value.copy(message = "Mit Notion synchronisiert · ${java.time.LocalTime.now().withSecond(0).withNano(0)}")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(message = "Lokal gespeichert · ${e.message ?: "Notion nicht erreichbar"}")
        } finally {
            _syncState.value = _syncState.value.copy(busy = false, pending = records.count { it.dirty })
        }
    }

    private fun applySaved(current: InventoryItem, snapshot: InventoryItem, sent: InventoryItem, pageId: String, savedCover: String?): InventoryItem {
        val coverSent = sent.coverPending && current.localCoverPath == sent.localCoverPath && current.coverUrl == sent.coverUrl
        val coverUrl = if (!sent.coverPending) current.coverUrl
            else if (coverSent) savedCover ?: if (sent.localCoverPath != null) null else sent.coverUrl
            else current.coverUrl
        return if (current == snapshot) sent.copy(pageId = pageId, dirty = false, coverPending = false, coverUrl = coverUrl)
        // Changed while uploading: keep it dirty, but remember the page so the next round updates instead of creating.
        else current.copy(pageId = current.pageId ?: pageId, coverPending = current.coverPending && !coverSent,
            coverUrl = if (coverSent) coverUrl else current.coverUrl)
    }
    fun syncOnResume() { if (configRepo.preferences.value.autoSyncEnabled) scope.launch { sync() } }
    companion object {
        @Volatile private var instance: InventoryRepository? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: InventoryRepository(context.applicationContext).also { instance = it }
        }
    }
}
