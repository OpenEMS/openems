package io.openems.edge.timeofusetariff.api.utils;

import static io.openems.common.utils.XmlUtils.getXmlRootDocument;
import static io.openems.common.utils.XmlUtils.stream;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.XmlUtils;
import io.openems.edge.common.currency.Currency;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * A utility class for fetching exchange rates from a web API.
 * 
 * <p>
 * Day ahead prices retrieved from ENTSO-E are usually in EUR and might have to
 * be converted to the user's currency using the exchange rates provided by
 * European Central Bank.
 */
// TODO this should be extracted to a Exchange-Rate API + Provider
public class ExchangeRateApi {

	private static final String ECB_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

	// ECB gives exchange rates based on the EUR.
	private static final Currency BASE_CURRENCY = Currency.EUR;

	private static final OkHttpClient client = new OkHttpClient();

	/**
	 * Fetches the exchange rate from ECB API.
	 * 
	 * @param source the source currency (e.g. EUR)
	 * @param target the target currency (e.g. SEK)
	 * @param orElse the default value
	 * @return the exchange rate.
	 */
	public static double getExchangeRateOrElse(String source, Currency target, double orElse) {
		try {
			return getExchangeRate(source, target);
		} catch (Exception e) {
			e.printStackTrace();
			return orElse;
		}
	}

	/**
	 * Fetches the exchange rate from ECB API.
	 * 
	 * @param source the source currency (e.g. EUR)
	 * @param target the target currency (e.g. SEK)
	 * @return the exchange rate.
	 * @throws IOException                  on error
	 * @throws OpenemsNamedException        on error
	 * @throws SAXException                 on error
	 * @throws ParserConfigurationException on error
	 */
	public static double getExchangeRate(String source, Currency target)
			throws IOException, OpenemsNamedException, ParserConfigurationException, SAXException {
		if (target == Currency.UNDEFINED) {
			throw new OpenemsException("Global Currency is UNDEFINED. Please configure it in Core.Meta component");
		}

		if (target.name().equals(source) || target.equals(BASE_CURRENCY)) {
			return 1.; // No need to fetch exchange rate from API
		}

		var request = new Request.Builder() //
				.url(ECB_URL) //
				.build();

		try (var response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Failed to fetch exchange rate. HTTP status code: " + response.code());
			}

			return parseResponse(response.body().string(), target);
		}
	}

	/**
	 * Parses the response string from ECB API.
	 * 
	 * @param response the response string
	 * @param target   the target currency (e.g. SEK)
	 * @return the exchange rate.
	 * @throws IOException                  on error.
	 * @throws SAXException                 on error.
	 * @throws ParserConfigurationException on error.
	 */
	protected static double parseResponse(String response, Currency target)
			throws ParserConfigurationException, SAXException, IOException {
		var root = getXmlRootDocument(response);
		return stream(root) //
				.filter(n -> n.getNodeName() == "Cube") // Filter all <Cube> elements
				.flatMap(XmlUtils::stream) // Stream the child nodes of each <Cube>
				.filter(n -> n.getNodeName().equals("Cube")) //
				.flatMap(XmlUtils::stream) //
				.filter(n -> n.getNodeName().equals("Cube")) //
				.filter(n -> n.getAttributes().getNamedItem("currency").getNodeValue().equals(target.name())) //
				.map(n -> n.getAttributes().getNamedItem("rate").getNodeValue()) //
				.mapToDouble(Double::parseDouble) //
				.findFirst() //
				.getAsDouble();
	}

}
