package io.vev.backend.edge2kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.WakeupException;

/**
 * Encapsulates Kafka producer and consumer lifecycle management, keeping the
 * Edge2Kafka bridge focused on transforming OpenEMS events.
 */
final class KafkaClient {

	private final boolean producerEnabled;
	private final boolean consumerEnabled;
	private final String dataTopic;
	private final String commandTopic;
	private final String consumerGroupId;
	private final Properties producerProperties;
	private final Properties consumerProperties;
	private final Duration pollInterval;
	private final long shutdownTimeoutMs;
	private final Consumer<ConsumerRecord<String, String>> commandHandler;
	private final Consumer<String> infoLogger;
	private final Consumer<String> warnLogger;
	private final String componentId;

	private KafkaProducer<String, String> producer;
	private KafkaConsumer<String, String> consumer;
	private ExecutorService consumerExecutor;
	private Future<?> consumerFuture;
	private final AtomicBoolean consumerRunning = new AtomicBoolean(false);

	KafkaClient(boolean producerEnabled, boolean consumerEnabled, String dataTopic, String commandTopic,
			String consumerGroupId, Properties producerProperties, Properties consumerProperties, Duration pollInterval,
			long shutdownTimeoutMs, Consumer<ConsumerRecord<String, String>> commandHandler,
			Consumer<String> infoLogger, Consumer<String> warnLogger, String componentId) {

		this.producerEnabled = producerEnabled;
		this.consumerEnabled = consumerEnabled;
		this.dataTopic = Objects.requireNonNull(dataTopic, "dataTopic");
		this.commandTopic = Objects.requireNonNull(commandTopic, "commandTopic");
		this.consumerGroupId = Objects.requireNonNull(consumerGroupId, "consumerGroupId");
		this.producerProperties = producerProperties;
		this.consumerProperties = consumerProperties;
		this.pollInterval = Objects.requireNonNull(pollInterval, "pollInterval");
		this.shutdownTimeoutMs = shutdownTimeoutMs;
		this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
		this.infoLogger = Objects.requireNonNull(infoLogger, "infoLogger");
		this.warnLogger = Objects.requireNonNull(warnLogger, "warnLogger");
		this.componentId = Objects.requireNonNull(componentId, "componentId");
	}

	void start() {
		if (this.producerEnabled) {
			try {
				this.producer = new KafkaProducer<>(this.producerProperties);
				this.infoLogger.accept("Kafka producer ready [topic=%s]".formatted(this.dataTopic));
			} catch (KafkaException e) {
				this.producer = null;
				this.warnLogger.accept("Unable to start Kafka producer: " + e.getMessage());
			}
		} else {
			this.infoLogger.accept("Kafka producer disabled");
		}

		if (this.consumerEnabled) {
			try {
				var consumer = new KafkaConsumer<String, String>(this.consumerProperties);
				consumer.subscribe(Collections.singletonList(this.commandTopic));
				this.consumer = consumer;
				this.startConsumerLoop(consumer);
				this.infoLogger.accept("Kafka consumer listening [topic=%s, groupId=%s]".formatted(this.commandTopic,
						this.consumerGroupId));
			} catch (KafkaException e) {
				this.warnLogger.accept("Unable to start Kafka consumer: " + e.getMessage());
				this.stopConsumer();
			}
		} else {
			this.infoLogger.accept("Kafka consumer disabled");
		}
	}

	void publish(String key, Collection<String> payloads) {
		if (!this.producerEnabled || this.producer == null || payloads == null || payloads.isEmpty()) {
			return;
		}

		for (var payload : payloads) {
			var record = new ProducerRecord<>(this.dataTopic, key, payload);
			this.producer.send(record, (metadata, exception) -> {
				if (exception != null) {
					this.warnLogger.accept("Failed to publish Kafka record: " + exception.getMessage());
				}
			});
		}
	}

	void stop() {
		this.stopConsumer();
		this.closeProducer();
	}

	private void startConsumerLoop(KafkaConsumer<String, String> kafkaConsumer) {
		this.consumerExecutor = Executors.newSingleThreadExecutor(r -> {
			var thread = new Thread(r, "edge2kafka-consumer-" + this.componentId);
			thread.setDaemon(true);
			return thread;
		});
		this.consumerRunning.set(true);
		this.consumerFuture = this.consumerExecutor.submit(() -> this.consumeCommands(kafkaConsumer));
	}

	private void consumeCommands(KafkaConsumer<String, String> kafkaConsumer) {
		try {
			while (this.consumerRunning.get()) {
				var records = kafkaConsumer.poll(this.pollInterval);
				for (var record : records) {
					this.commandHandler.accept(record);
				}
			}
		} catch (WakeupException e) {
			// Expected on shutdown.
		} catch (Exception e) {
			this.warnLogger.accept("Kafka consumer error: " + e.getMessage());
		}
	}

	private void stopConsumer() {
		this.consumerRunning.set(false);

		var consumerRef = this.consumer;
		this.consumer = null;

		if (consumerRef != null) {
			try {
				consumerRef.wakeup();
			} catch (IllegalStateException e) {
				// Ignore, consumer not yet started.
			}
		}

		if (this.consumerFuture != null) {
			this.consumerFuture.cancel(true);
			this.consumerFuture = null;
		}

		if (this.consumerExecutor != null) {
			this.consumerExecutor.shutdown();
			try {
				if (!this.consumerExecutor.awaitTermination(this.shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
					this.consumerExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				this.consumerExecutor.shutdownNow();
			}
			this.consumerExecutor = null;
		}

		if (consumerRef != null) {
			try {
				consumerRef.close();
			} catch (Exception e) {
				this.warnLogger.accept("Kafka consumer close failed: " + e.getMessage());
			}
		}
	}

	private void closeProducer() {
		var producerRef = this.producer;
		this.producer = null;

		if (producerRef == null) {
			return;
		}

		try {
			producerRef.flush();
		} catch (Exception e) {
			this.warnLogger.accept("Kafka producer flush failed: " + e.getMessage());
		}

		try {
			producerRef.close();
		} catch (Exception e) {
			this.warnLogger.accept("Kafka producer close failed: " + e.getMessage());
		}
	}
}
