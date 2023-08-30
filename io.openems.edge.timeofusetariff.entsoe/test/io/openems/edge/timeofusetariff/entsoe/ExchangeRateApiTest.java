package io.openems.edge.timeofusetariff.entsoe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.currency.Currency;

public class ExchangeRateApiTest {

	private static final String EXCHANGE_DATA = """
			{
				"success": true,
				"base": "EUR",
				"date": "2023-07-27",
				"rates": {
					"AED": 4.074263,
					"AFN": 96.546557,
					"ALL": 102.049538,
					"AMD": 431.402948,
					"ANG": 2.00055,
					"AOA": 916.112472,
					"ARS": 302.239867,
					"AUD": 1.629808,
					"AWG": 1.998636,
					"AZN": 1.886792,
					"BAM": 1.958547,
					"BBD": 2.219307,
					"BDT": 120.446245,
					"BGN": 1.955529,
					"BHD": 0.418333,
					"BIF": 3141.458211,
					"BMD": 1.109917,
					"BND": 1.473742,
					"BOB": 7.669034,
					"BRL": 5.256284,
					"BSD": 1.109668,
					"BTC": 0.000038,
					"BTN": 91.002709,
					"BWP": 14.488226,
					"BYN": 2.802141,
					"BZD": 2.237335,
					"CAD": 1.46224,
					"CDF": 2828.550254,
					"CHF": 0.954345,
					"CLF": 0.033979,
					"CLP": 914.830807,
					"CNH": 7.91491,
					"CNY": 7.914035,
					"COP": 4400.287374,
					"CRC": 594.125915,
					"CUC": 1.110171,
					"CUP": 28.563636,
					"CVE": 110.417189,
					"CZK": 24.025043,
					"DJF": 197.373766,
					"DKK": 7.448692,
					"DOP": 62.137242,
					"DZD": 149.842018,
					"EGP": 34.274712,
					"ERN": 16.639118,
					"ETB": 60.709379,
					"EUR": 1,
					"FJD": 2.461322,
					"FKP": 0.857521,
					"GBP": 0.857065,
					"GEL": 2.879232,
					"GGP": 0.856846,
					"GHS": 12.563656,
					"GIP": 0.857444,
					"GMD": 66.111378,
					"GNF": 9545.125955,
					"GTQ": 8.711891,
					"GYD": 232.184942,
					"HKD": 8.646912,
					"HNL": 27.325849,
					"HRK": 7.53181,
					"HTG": 152.038911,
					"HUF": 381.18337,
					"IDR": 16642.2715,
					"ILS": 4.089653,
					"IMP": 0.856722,
					"INR": 90.963493,
					"IQD": 1453.098308,
					"IRR": 46879.045396,
					"ISK": 145.620797,
					"JEP": 0.857125,
					"JMD": 171.374964,
					"JOD": 0.787314,
					"JPY": 155.307802,
					"KES": 157.73419,
					"KGS": 97.449403,
					"KHR": 4577.516015,
					"KMF": 493.194214,
					"KPW": 998.312097,
					"KRW": 1415.953271,
					"KWD": 0.340683,
					"KYD": 0.924915,
					"KZT": 493.937153,
					"LAK": 21383.452835,
					"LBP": 16776.637925,
					"LKR": 366.792902,
					"LRD": 205.652382,
					"LSL": 19.598023,
					"LYD": 5.294188,
					"MAD": 10.787573,
					"MDL": 19.474446,
					"MGA": 4984.515277,
					"MKD": 61.641818,
					"MMK": 2330.490351,
					"MNT": 3903.398698,
					"MOP": 8.922849,
					"MRU": 37.929989,
					"MUR": 50.325954,
					"MVR": 17.027553,
					"MWK": 1168.023721,
					"MXN": 18.676827,
					"MYR": 5.024866,
					"MZN": 70.714101,
					"NAD": 19.579235,
					"NGN": 874.632255,
					"NIO": 40.574721,
					"NOK": 11.166941,
					"NPR": 145.603541,
					"NZD": 1.775313,
					"OMR": 0.427697,
					"PAB": 1.110007,
					"PEN": 3.990703,
					"PGK": 3.980789,
					"PHP": 60.551875,
					"PKR": 318.444017,
					"PLN": 4.41964,
					"PYG": 8080.356808,
					"QAR": 4.039405,
					"RON": 4.923793,
					"RSD": 117.135798,
					"RUB": 99.859233,
					"RWF": 1302.494419,
					"SAR": 4.160461,
					"SBD": 9.298905,
					"SCR": 14.821421,
					"SDG": 667.205497,
					"SEK": 11.504976,
					"SGD": 1.469025,
					"SHP": 0.857181,
					"SLL": 19594.637867,
					"SOS": 631.983688,
					"SRD": 42.561687,
					"SSP": 144.489173,
					"STD": 25317.170823,
					"STN": 24.847059,
					"SVC": 9.710443,
					"SYP": 2786.987107,
					"SZL": 19.625727,
					"THB": 37.81308,
					"TJS": 12.130966,
					"TMT": 3.882521,
					"TND": 3.428976,
					"TOP": 2.599614,
					"TRY": 29.846988,
					"TTD": 7.539697,
					"TWD": 34.634021,
					"TZS": 2717.626941,
					"UAH": 40.986807,
					"UGX": 4049.121605,
					"USD": 1.109987,
					"UYU": 42.02786,
					"UZS": 12905.949812,
					"VES": 32.209006,
					"VND": 26260.304612,
					"VUV": 131.976192,
					"WST": 3.024415,
					"XAF": 655.638919,
					"XAG": 0.044704,
					"XAU": 0.001581,
					"XCD": 2.997771,
					"XDR": 0.825364,
					"XOF": 655.639063,
					"XPD": 0.001494,
					"XPF": 119.274762,
					"XPT": 0.001957,
					"YER": 277.641934,
					"ZAR": 19.492845,
					"ZMW": 21.436533,
					"ZWL": 357.173757
				}
			}
			""";

	@Test
	@Ignore
	public void testExchangeRateApi() throws IOException {
		ExchangeRateApi.getExchangeRates();
	}

	@Test
	public void testExchangeRateParser() throws OpenemsNamedException {

		var currency = Currency.EUR;
		var response = Utils.exchangeRateParser(EXCHANGE_DATA, currency);

		assertTrue(response == 1.0);

		currency = Currency.SEK;
		response = Utils.exchangeRateParser(EXCHANGE_DATA, currency);

		assertFalse(response == 1.0);
	}
}
