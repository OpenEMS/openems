package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.Nameable;

public class LazySelectBuilder extends FormlyBuilder<LazySelectBuilder> {

	private record RequestParams(String componentId, String method) {
	}

	private RequestParams requestParams;
	private String loadingText;
	private String retryLoadingText;
	private String missingOptionsText;

	public LazySelectBuilder(Nameable property) {
		super(property);
	}

	@Override
	protected String getType() {
		return "lazy-select";
	}

	public LazySelectBuilder setRequestParams(String componentId, String method) {
		this.requestParams = new RequestParams(componentId, method);
		return this.self();
	}

	public LazySelectBuilder setLoadingText(String loadingText) {
		this.loadingText = loadingText;
		return this.self();
	}

	public LazySelectBuilder setRetryLoadingText(String retryLoadingText) {
		this.retryLoadingText = retryLoadingText;
		return this.self();
	}

	public LazySelectBuilder setMissingOptionsText(String missingOptionsText) {
		this.missingOptionsText = missingOptionsText;
		return this.self();
	}

	@Override
	public JsonObject build() {
		if (this.requestParams != null) {
			this.templateOptions.addProperty("componentId", this.requestParams.componentId());
			this.templateOptions.addProperty("method", this.requestParams.method());
		}
		if (this.loadingText != null) {
			this.templateOptions.addProperty("loadingText", this.loadingText);
		}
		if (this.retryLoadingText != null) {
			this.templateOptions.addProperty("retryLoadingText", this.retryLoadingText);
		}
		if (this.missingOptionsText != null) {
			this.templateOptions.addProperty("missingOptionsText", this.missingOptionsText);
		}
		return super.build();
	}
}
