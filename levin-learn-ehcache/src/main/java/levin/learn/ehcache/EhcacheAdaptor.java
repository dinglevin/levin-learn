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

	@Override
	public void unpinAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPinned(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPinned(Object key, boolean pinned) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Collection<Element> elements)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(Element element, boolean doNotNotifyCacheReplicators)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putQuiet(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putWithWriter(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element putIfAbsent(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element putIfAbsent(Element element,
			boolean doNotNotifyCacheReplicators) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeElement(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(Element old, Element element)
			throws NullPointerException, IllegalArgumentException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element replace(Element element) throws NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element get(Serializable key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element get(Object key) throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Object, Element> getAll(Collection<?> keys)
			throws IllegalStateException, CacheException, NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getQuiet(Serializable key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getQuiet(Object key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<?> getKeys() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<?> getKeysWithExpiryCheck() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<?> getKeysNoDuplicateCheck() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Serializable key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll(Collection<?> keys) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll(Collection<?> keys,
			boolean doNotNotifyCacheReplicators) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object key, boolean doNotNotifyCacheReplicators)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeQuiet(Serializable key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeQuiet(Object key) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeWithWriter(Object key) throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll(boolean doNotNotifyCacheReplicators)
			throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSize() throws IllegalStateException, CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSizeBasedOnAccuracy(int statisticsAccuracy)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long calculateInMemorySize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long calculateOffHeapSize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long calculateOnDiskSize() throws IllegalStateException,
			CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAbortedSizeOf() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getMemoryStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getOffHeapStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDiskStoreSize() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Status getStatus() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isExpired(Element element) throws IllegalStateException,
			NullPointerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public RegisteredEventListeners getCacheEventNotificationService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isElementInMemory(Serializable key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isElementInMemory(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isElementOnDisk(Serializable key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isElementOnDisk(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getGuid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CacheManager getCacheManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearStatistics() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getStatisticsAccuracy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStatisticsAccuracy(int statisticsAccuracy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void evictExpiredElements() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isKeyInCache(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isValueInCache(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Statistics getStatistics() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public LiveCacheStatistics getLiveCacheStatistics()
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerCacheUsageListener(CacheUsageListener cacheUsageListener)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeCacheUsageListener(CacheUsageListener cacheUsageListener)
			throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCacheManager(CacheManager cacheManager) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BootstrapCacheLoader getBootstrapCacheLoader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBootstrapCacheLoader(
			BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initialise() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void bootstrap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public CacheConfiguration getCacheConfiguration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerCacheExtension(CacheExtension cacheExtension) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unregisterCacheExtension(CacheExtension cacheExtension) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CacheExtension> getRegisteredCacheExtensions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getAverageGetTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCacheExceptionHandler(
			CacheExceptionHandler cacheExceptionHandler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CacheExceptionHandler getCacheExceptionHandler() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerCacheLoader(CacheLoader cacheLoader) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unregisterCacheLoader(CacheLoader cacheLoader) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CacheLoader> getRegisteredCacheLoaders() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerDynamicAttributesExtractor(
			DynamicAttributesExtractor extractor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerCacheWriter(CacheWriter cacheWriter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unregisterCacheWriter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CacheWriter getRegisteredCacheWriter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getWithLoader(Object key, CacheLoader loader,
			Object loaderArgument) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<?, ?> getAllWithLoader(Collection keys, Object loaderArgument)
			throws CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void load(Object key) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void loadAll(Collection keys, Object argument) throws CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDisabled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDisabled(boolean disabled) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isStatisticsEnabled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStatisticsEnabled(boolean enableStatistics) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SampledCacheStatistics getSampledCacheStatistics() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSampledStatisticsEnabled(boolean enableStatistics) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSampledStatisticsEnabled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getInternalContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disableDynamicFeatures() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CacheWriterManager getWriterManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isClusterCoherent() throws TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNodeCoherent() throws TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeCoherent(boolean coherent)
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void waitUntilClusterCoherent()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTransactionManagerLookup(
			TransactionManagerLookup transactionManagerLookup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Attribute<T> getSearchAttribute(String attributeName)
			throws CacheException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Query createQuery() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSearchable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getAverageSearchTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getSearchesPerSecond() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void acquireReadLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void acquireWriteLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean tryReadLockOnKey(Object key, long timeout)
			throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean tryWriteLockOnKey(Object key, long timeout)
			throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void releaseReadLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void releaseWriteLockOnKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadLockedByCurrentThread(Object key)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWriteLockedByCurrentThread(Object key)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isClusterBulkLoadEnabled()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNodeBulkLoadEnabled()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeBulkLoadEnabled(boolean enabledBulkLoad)
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void waitUntilClusterBulkLoadComplete()
			throws UnsupportedOperationException, TerracottaNotRunningException {
		throw new UnsupportedOperationException();
	}
	
	public Object clone() throws CloneNotSupportedException {
		return this;
	}

}

