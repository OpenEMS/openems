package io.openems.backend.core.services;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.services.currency.AbstractCurrencyExchange;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.services.currency.CurrencyExchangeApi;
import io.openems.common.services.currency.ExchangeRate;

@Component(service = CurrencyExchange.class)
public class BackendCurrencyExchange extends AbstractCurrencyExchange {

	@Reference(cardinality = MANDATORY, policy = DYNAMIC, policyOption = GREEDY)
	private void bindCurrencyExchangeApi(CurrencyExchangeApi currencyExchangeApi) {
		currencyExchangeApi.getExchangeRates().subscribe(this.onNewExchangeRatesFunc);
	}

	private void unbindCurrencyExchangeApi(CurrencyExchangeApi currencyExchangeApi) {
		currencyExchangeApi.getExchangeRates().unsubscribe(this.onNewExchangeRatesFunc);
	}

	private final Consumer<List<ExchangeRate>> onNewExchangeRatesFunc = this::onNewExchangeRates;

	protected void onNewExchangeRates(List<ExchangeRate> exchangeRates) {
		this.setExchangeRates(exchangeRates);
	}
}
