package io.openems.backend.edge.manager.syncable;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Instant;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.syncable.types.SyncableExchangeRateData;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.services.currency.ExchangeRate;

@Component(immediate = true, service = CurrencyExchangeSyncManager.class)
public class CurrencyExchangeSyncManager {
	@Reference(cardinality = MANDATORY, policy = STATIC, policyOption = GREEDY)
	private CurrencyExchange currencyExchange;

	@Reference
	private SyncableRepository syncableRepository;

	private final Consumer<Void> onExchangeRatesChangeFunc = v -> this.writeRatesIntoSyncable();

	@Activate
	protected void activate() {
		this.writeRatesIntoSyncable();
		this.currencyExchange.onExchangeRatesChanged().subscribe(this.onExchangeRatesChangeFunc);
	}

	@Deactivate
	protected void deactivate() {
		this.currencyExchange.onExchangeRatesChanged().unsubscribe(this.onExchangeRatesChangeFunc);
	}

	private void writeRatesIntoSyncable() {
		var exchangeRates = this.currencyExchange.getAllExchangeRates().toArray(ExchangeRate[]::new);
		var syncableData = new SyncableExchangeRateData(exchangeRates);
		this.syncableRepository.setStaticEntry(
				new SyncableEntry<>(new SyncableKey.CurrencyExchangeRates(), syncableData, Instant.now()));
	}
}
