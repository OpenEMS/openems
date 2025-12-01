package io.openems.edge.evcc.loadpoint;

import org.slf4j.Logger;

import io.openems.edge.common.component.AbstractOpenemsComponent;

/**
 * Abstract base class for EVCC loadpoint meters.
 *
 * <p>
 * Provides common functionality for identifying and selecting loadpoints from
 * EVCC API using a combination of title and index-based fallback strategy.
 */
public abstract class AbstractLoadpointMeterEvcc extends AbstractOpenemsComponent {

	private String configuredTitle;
	private int configuredIndex;
	private boolean fallbackWarningLogged = false;

	protected AbstractLoadpointMeterEvcc(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Initializes the loadpoint reference configuration.
	 *
	 * @param title the configured loadpoint title (can be empty)
	 * @param index the configured loadpoint index
	 */
	protected void initializeLoadpointReference(String title, int index) {
		this.configuredTitle = title;
		this.configuredIndex = index;
	}

	/**
	 * Builds a JQ filter to select the loadpoint.
	 *
	 * <p>
	 * Strategy (from most specific to most generic):
	 * <ol>
	 * <li>If title is provided: (.loadpoints[] | select(.title == "X")) // .loadpoints[N]
	 * <li>This means: try title first, if not found use index as fallback
	 * <li>If title is empty: use index only
	 * </ol>
	 *
	 * @param title the configured loadpoint title
	 * @param index the configured loadpoint index
	 * @return JQ filter expression
	 */
	protected String buildLoadpointFilter(String title, int index) {
		if (title != null && !title.trim().isEmpty()) {
			// Use title with index as fallback
			// JQ: (.loadpoints[] | select(.title == "Carport")) // .loadpoints[0]
			// Escape special characters for JQ string literal
			var escapedTitle = title.trim().replace("\\", "\\\\").replace("\"", "\\\"");
			return "(.loadpoints[] | select(.title == \"" + escapedTitle + "\")) // .loadpoints[" + index + "]";
		}
		// Use index only
		return ".loadpoints[" + index + "]";
	}

	/**
	 * Checks if the received loadpoint matches the configured title and index
	 * combination.
	 *
	 * <p>
	 * Warns if:
	 * <ul>
	 * <li>Title is configured but doesn't match → fallback to index was used
	 * <li>Both title and index are configured but only one matches → partial match
	 * </ul>
	 *
	 * @param lp     the loadpoint JSON object
	 * @param logger the logger to use for warnings
	 */
	protected void checkLoadpointMatch(com.google.gson.JsonObject lp, Logger logger) {
		// Only check if title is configured
		if (this.configuredTitle == null || this.configuredTitle.trim().isEmpty()) {
			return;
		}

		// Check if loadpoint has title field
		if (!lp.has("title")) {
			return;
		}

		var actualTitle = lp.get("title").getAsString();

		// Warn once if the title doesn't match (meaning fallback to index was used)
		if (!this.configuredTitle.trim().equals(actualTitle) && !this.fallbackWarningLogged) {
			logger.warn(
					"Loadpoint title mismatch! Configured title='{}' + index=[{}], but received title='{}'. "
							+ "Using fallback to index. This means the loadpoint at index [{}] has a different title than expected. "
							+ "Consider updating the configuration if EVCC loadpoints were reordered.",
					this.configuredTitle, this.configuredIndex, actualTitle, this.configuredIndex);
			this.fallbackWarningLogged = true;
		}
	}
}
