package levin.learn.ehcache;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;

public class EhcacheAdaptor implements Ehcache {

	public void unpinAll() {
		throw new UnsupportedOperationException();
	}

	public boolean isPinned(Object key) {
		throw new UnsupportedOperationException();
	}

	public void setPinned(Object key, boolean pinned) {
		throw new UnsupportedOperationException();
	}

	public void put(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public void putAll(Collection<Element> elements)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public void put(Element element, boolean doNotNotifyCacheReplicators)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public void putQuiet(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public void putWithWriter(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public Element putIfAbsent(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	public Element putIfAbsent(Element element,
			boolean doNotNotifyCacheReplicators) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	public boolean removeElement(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	public boolean replace(Element old, Element element)
			throws NullPointerException, IllegalArgumentException {
		throw new UnsupportedOperationException();
	}

	public Element replace(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	public Element get(Serializable key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public Element get(Object key) throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public Map<Object, Element> getAll(Collection<?> keys)
			throws IllegalStateException, CacheException, NullPointerException {
		throw new UnsupportedOperationException();
	}

	public Element getQuiet(Serializable key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public Element getQuiet(Object key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public List<?> getKeys() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public List<?> getKeysWithExpiryCheck() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public List<?> getKeysNoDuplicateCheck() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Serializable key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public void removeAll(Collection<?> keys) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	public void removeAll(Collection<?> keys,
			boolean doNotNotifyCacheReplicators) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object key, boolean doNotNotifyCacheReplicators)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean removeQuiet(Serializable key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean removeQuiet(Object key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public boolean removeWithWriter(Object key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public void removeAll() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public void removeAll(boolean doNotNotifyCacheReplicators)
			throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public void flush() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public int getSize() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	public int getSizeBasedOnAccuracy(int statisticsAccuracy)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public long calculateInMemorySize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public long calculateOffHeapSize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public long calculateOnDiskSize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	public boolean hasAbortedSizeOf() {
		throw new UnsupportedOperationException();
	}

	public long getMemoryStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public long getOffHeapStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public int getDiskStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public Status getStatus() {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		throw new UnsupportedOperationException();
	}

	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	public boolean isExpired(Element element) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	public RegisteredEventListeners getCacheEventNotificationService() {
		throw new UnsupportedOperationException();
	}

	public boolean isElementInMemory(Serializable key) {
		throw new UnsupportedOperationException();
	}

	public boolean isElementInMemory(Object key) {
		throw new UnsupportedOperationException();
	}

	public boolean isElementOnDisk(Serializable key) {
		throw new UnsupportedOperationException();
	}

	public boolean isElementOnDisk(Object key) {
		throw new UnsupportedOperationException();
	}

	public String getGuid() {
		throw new UnsupportedOperationException();
	}

	public CacheManager getCacheManager() {
		throw new UnsupportedOperationException();
	}

	public void clearStatistics() {
		throw new UnsupportedOperationException();
	}

	public int getStatisticsAccuracy() {
		throw new UnsupportedOperationException();
	}

	public void setStatisticsAccuracy(int statisticsAccuracy) {
		throw new UnsupportedOperationException();
	}

	public void evictExpiredElements() {
		throw new UnsupportedOperationException();
	}

	public boolean isKeyInCache(Object key) {
		throw new UnsupportedOperationException();
	}

	public boolean isValueInCache(Object value) {
		throw new UnsupportedOperationException();
	}

	public Statistics getStatistics() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public LiveCacheStatistics getLiveCacheStatistics()
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public void registerCacheUsageListener(CacheUsageListener cacheUsageListener)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public void removeCacheUsageListener(CacheUsageListener cacheUsageListener)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public void setCacheManager(CacheManager cacheManager) {
		throw new UnsupportedOperationException();
	}

	public BootstrapCacheLoader getBootstrapCacheLoader() {
		throw new UnsupportedOperationException();
	}

	public void setBootstrapCacheLoader(
			BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
		throw new UnsupportedOperationException();
	}

	public void initialise() {
		throw new UnsupportedOperationException();
	}

	public void bootstrap() {
		throw new UnsupportedOperationException();
	}

	public void dispose() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public CacheConfiguration getCacheConfiguration() {
		throw new UnsupportedOperationException();
	}

	public void registerCacheExtension(CacheExtension cacheExtension) {
		throw new UnsupportedOperationException();
	}

	public void unregisterCacheExtension(CacheExtension cacheExtension) {
		throw new UnsupportedOperationException();
	}

	public List<CacheExtension> getRegisteredCacheExtensions() {
		throw new UnsupportedOperationException();
	}

	public float getAverageGetTime() {
		throw new UnsupportedOperationException();
	}

	public void setCacheExceptionHandler(
			CacheExceptionHandler cacheExceptionHandler) {
		throw new UnsupportedOperationException();
	}

	public CacheExceptionHandler getCacheExceptionHandler() {
		throw new UnsupportedOperationException();
	}

	public void registerCacheLoader(CacheLoader cacheLoader) {
		throw new UnsupportedOperationException();
	}

	public void unregisterCacheLoader(CacheLoader cacheLoader) {
		throw new UnsupportedOperationException();
	}

	public List<CacheLoader> getRegisteredCacheLoaders() {
		throw new UnsupportedOperationException();
	}

	public void registerDynamicAttributesExtractor(
			DynamicAttributesExtractor extractor) {
		throw new UnsupportedOperationException();
	}

	public void registerCacheWriter(CacheWriter cacheWriter) {
		throw new UnsupportedOperationException();
	}

	public void unregisterCacheWriter() {
		throw new UnsupportedOperationException();
	}

	public CacheWriter getRegisteredCacheWriter() {
		throw new UnsupportedOperationException();
	}

	public Element getWithLoader(Object key, CacheLoader loader,
			Object loaderArgument) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	public Map<?, ?> getAllWithLoader(Collection keys, Object loaderArgument)
			throws CacheException {
		throw new UnsupportedOperationException();
	}

	public void load(Object key) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	public void loadAll(Collection keys, Object argument) throws CacheException {
		throw new UnsupportedOperationException();
	}

	public boolean isDisabled() {
		throw new UnsupportedOperationException();
	}

	public void setDisabled(boolean disabled) {
		throw new UnsupportedOperationException();
	}

	public boolean isStatisticsEnabled() {
		throw new UnsupportedOperationException();
	}

	public void setStatisticsEnabled(boolean enableStatistics) {
		throw new UnsupportedOperationException();
	}

	public SampledCacheStatistics getSampledCacheStatistics() {
		throw new UnsupportedOperationException();
	}

	public void setSampledStatisticsEnabled(boolean enableStatistics) {
		throw new UnsupportedOperationException();
	}

	public boolean isSampledStatisticsEnabled() {
		throw new UnsupportedOperationException();
	}

	public Object getInternalContext() {
		throw new UnsupportedOperationException();
	}

	public void disableDynamicFeatures() {
		throw new UnsupportedOperationException();
	}

	public CacheWriterManager getWriterManager() {
		throw new UnsupportedOperationException();
	}

	public boolean isClusterCoherent() throws TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public boolean isNodeCoherent() throws TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public void setNodeCoherent(boolean coherent)
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public void waitUntilClusterCoherent()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public void setTransactionManagerLookup(
			TransactionManagerLookup transactionManagerLookup) {
		throw new UnsupportedOperationException();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	public <T> Attribute<T> getSearchAttribute(String attributeName)
			throws CacheException {
		throw new UnsupportedOperationException();
	}

	public Query createQuery() {
		throw new UnsupportedOperationException();
	}

	public boolean isSearchable() {
		throw new UnsupportedOperationException();
	}

	public long getAverageSearchTime() {
		throw new UnsupportedOperationException();
	}

	public long getSearchesPerSecond() {
		throw new UnsupportedOperationException();
	}

	public void acquireReadLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	public void acquireWriteLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	public boolean tryReadLockOnKey(Object key, long timeout)
			throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	public boolean tryWriteLockOnKey(Object key, long timeout)
			throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	public void releaseReadLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	public void releaseWriteLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	public boolean isReadLockedByCurrentThread(Object key)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public boolean isWriteLockedByCurrentThread(Object key)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public boolean isClusterBulkLoadEnabled()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public boolean isNodeBulkLoadEnabled()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public void setNodeBulkLoadEnabled(boolean enabledBulkLoad)
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	public void waitUntilClusterBulkLoadComplete()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}
	
	public Object clone() throws CloneNotSupportedException {
		return this;
	}

}

