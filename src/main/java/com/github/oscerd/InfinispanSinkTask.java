package com.github.oscerd;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfinispanSinkTask extends SinkTask {
	private static Logger log = LoggerFactory.getLogger(InfinispanSinkTask.class);
	private RemoteCacheManager cacheManager;
	private RemoteCache<String, String> cache;
	private InfinispanSinkConnectorConfig config;

	@Override
	public String version() {
		return VersionUtil.getVersion();
	}

	@Override
	public void start(Map<String, String> map) {
		// Create a configuration for a locally-running server
		config = new InfinispanSinkConnectorConfig(map);
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer()
		        .host(config.getString(InfinispanSinkConnectorConfig.INFINISPAN_CONNECTION_HOSTS_CONF))
				.port(config.getInt(InfinispanSinkConnectorConfig.INFINISPAN_CONNECTION_HOTROD_PORT_CONF));
		// Connect to the server
		cacheManager = new RemoteCacheManager(builder.build());
		cache = cacheManager.getCache(config.getString(InfinispanSinkConnectorConfig.INFINISPAN_CONNECTION_CACHE_NAME_CONF));
	}

	@Override
	public void put(Collection<SinkRecord> collection) {
		if (collection.isEmpty()) {
			return;
		}
		final int recordsCount = collection.size();
		log.info("Received {} records", recordsCount);
		Iterator it = collection.iterator();
		while (it.hasNext()) {
			SinkRecord record = (SinkRecord) it.next();
			log.info("Record kafka coordinates:({}-{}-{}). Writing it to Infinispan...", record.topic(), record.key(), record.value());
			cache.put(record.key().toString(), record.value().toString());
		}
	}

	@Override
	public void flush(Map<TopicPartition, OffsetAndMetadata> map) {

	}

	@Override
	public void stop() {
		cacheManager.stop();
	}

}