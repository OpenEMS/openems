package io.openems.edge.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.syncable.types.SyncableExchangeRateData;
import io.openems.common.services.currency.CurrencyExchangeApi;
import io.openems.common.services.currency.DummyCurrencyExchange;
import io.openems.common.services.currency.ExchangeRate;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.common.services.syncable.DummySyncableSubscription;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.controller.api.backend.api.DataFetchType;
import io.openems.edge.controller.api.backend.services.BackendConnectionStateListener;

class EdgeCurrencyExchangeTest {
	private final BehaviorSubject<DataFetchType> dataFetchType = new BehaviorSubject<>(DataFetchType.WAITING);
	private CurrencyExchangeApi currencyExchangeApi;
	private SyncableRepository syncableRepository;
	private EdgeCurrencyExchange currencyExchange;

	@BeforeEach
	void setUp() {
		final var dummyBackendConnectionStateListener = mock(BackendConnectionStateListener.class);
		when(dummyBackendConnectionStateListener.getDataFetchType()).thenReturn(this.dataFetchType);

		final var dummyCurrencyExchange = new DummyCurrencyExchange();
		dummyCurrencyExchange.addBasicConversions();

		this.currencyExchangeApi = mock(CurrencyExchangeApi.class);
		when(this.currencyExchangeApi.getExchangeRates())
				.thenReturn(new BehaviorSubject<>(dummyCurrencyExchange.getAllExchangeRates()));

		final var syncableExchangeRateEntry = new SyncableEntry<>(//
				new SyncableKey.CurrencyExchangeRates(), //
				new SyncableExchangeRateData(dummyCurrencyExchange.getAllExchangeRates().toArray(ExchangeRate[]::new)), //
				Instant.now());

		this.syncableRepository = mock(SyncableRepository.class);
		when(this.syncableRepository.subscribe(new SyncableKey.CurrencyExchangeRates()))
				.thenReturn(new DummySyncableSubscription<>(syncableExchangeRateEntry));

		this.currencyExchange = new EdgeCurrencyExchange() {
			@Override
			protected void loadApiReference() {
				this.api = EdgeCurrencyExchangeTest.this.currencyExchangeApi;
			}
		};
		this.currencyExchange.syncableRepository = this.syncableRepository;
		this.currencyExchange.backendConnectionStateListener = dummyBackendConnectionStateListener;
		this.currencyExchange.activate(null);
	}

	@Test
	void testLocally() {
		assertTrue(this.currencyExchange.getAllExchangeRates().isEmpty());

		verify(this.currencyExchangeApi, never()).getExchangeRates();

		this.dataFetchType.setValue(DataFetchType.FETCH_LOCALLY);
		verify(this.currencyExchangeApi, only()).getExchangeRates();
		assertFalse(this.currencyExchange.getAllExchangeRates().isEmpty());
		verify(this.syncableRepository, never()).subscribe(new SyncableKey.CurrencyExchangeRates());

		// Test that nothing changes with WAITING status
		this.dataFetchType.setValue(DataFetchType.WAITING);
		verify(this.currencyExchangeApi, only()).getExchangeRates();
		assertFalse(this.currencyExchange.getAllExchangeRates().isEmpty());
		verify(this.syncableRepository, never()).subscribe(new SyncableKey.CurrencyExchangeRates());

		// Switch to backend
		this.dataFetchType.setValue(DataFetchType.FETCH_VIA_BACKEND);
		verify(this.syncableRepository, only()).subscribe(new SyncableKey.CurrencyExchangeRates());
	}

	@Test
	void testBackend() {
		assertTrue(this.currencyExchange.getAllExchangeRates().isEmpty());
		verify(this.syncableRepository, never()).subscribe(new SyncableKey.CurrencyExchangeRates());

		this.dataFetchType.setValue(DataFetchType.FETCH_VIA_BACKEND);
		verify(this.currencyExchangeApi, never()).getExchangeRates();
		assertFalse(this.currencyExchange.getAllExchangeRates().isEmpty());
		verify(this.syncableRepository, only()).subscribe(new SyncableKey.CurrencyExchangeRates());

		// Test that nothing changes with WAITING status
		this.dataFetchType.setValue(DataFetchType.WAITING);
		verify(this.currencyExchangeApi, never()).getExchangeRates();
		assertFalse(this.currencyExchange.getAllExchangeRates().isEmpty());
		verify(this.syncableRepository, only()).subscribe(new SyncableKey.CurrencyExchangeRates());
		assertEquals(1, this.syncableRepository.subscribe(new SyncableKey.CurrencyExchangeRates()).getEntry()
				.getAmountOfSubscribers());

		// Switch to locally
		this.dataFetchType.setValue(DataFetchType.FETCH_LOCALLY);
		verify(this.syncableRepository, times(2)).subscribe(new SyncableKey.CurrencyExchangeRates());
		assertEquals(0, this.syncableRepository.subscribe(new SyncableKey.CurrencyExchangeRates()).getEntry()
				.getAmountOfSubscribers());
		verify(this.currencyExchangeApi, only()).getExchangeRates();
		assertFalse(this.currencyExchange.getAllExchangeRates().isEmpty());
	}
}
