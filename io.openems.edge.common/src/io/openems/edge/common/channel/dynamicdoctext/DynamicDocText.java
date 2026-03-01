package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.type.TextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;

public class DynamicDocText {
	private final Logger log = LoggerFactory.getLogger(DynamicDocText.class);

	private final TextProvider textFormat;
	private final ParameterProvider[] parameterProviders;

	private String channelId;

	public DynamicDocText(TextProvider textFormat, ParameterProvider[] parameterProviders) {
		this.textFormat = textFormat;
		this.parameterProviders = parameterProviders;
	}

	/**
	 * Initializes this dynamicDocText. It links all parameterProviders to the
	 * corresponding channel values and registers the value update method. Only call
	 * this once!
	 * 
	 * @param channel The channel to register in
	 */
	public void init(Channel<?> channel) {
		this.channelId = channel.channelId().name();

		var component = channel.getComponent();
		for (var parameterProvider : this.parameterProviders) {
			parameterProvider.init(component);
		}
	}

	/**
	 * Calculates the current text and returns it.
	 * 
	 * @param lang Language used for translation fetching
	 * @return Calculated text as string
	 */
	public String getText(Language lang) {
		var textFormat = this.textFormat.getText(lang);
		try {
			return MessageFormat.format(textFormat,
					Arrays.stream(this.parameterProviders).map(p -> p.getText(lang)).toArray());
		} catch (Exception ex) {
			this.log.error(
					"Failed to calculate dynamic text for " + this.channelId + " with format '" + textFormat + "'", ex);
			return textFormat;
		}
	}
}
