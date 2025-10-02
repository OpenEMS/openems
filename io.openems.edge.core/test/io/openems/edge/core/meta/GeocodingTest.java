package io.openems.edge.core.meta;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.google.gson.JsonParser;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.CurrencyConfig;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.meta.types.CountryCode;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.core.meta.geocoding.GeoResult;
import io.openems.edge.core.meta.geocoding.OpenCageGeocodingService;

public class GeocodingTest {

	@Test
	public void testGeocodeRequest_ShouldReturnCorrectResults() throws Exception {
		final var meta = new DummyMeta("_meta");
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		oem.withOpenCageApiKey("TEST-API-KEY");

		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> HttpResponse.ok(OPEN_CAGE_API_RESPONSE));
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new MetaImpl();

		final var config = MyConfig.create()//
				.setMeta(meta) //
				.setCurrency(CurrencyConfig.EUR) //
				.build();
		new ComponentTest(sut)//
				.addReference("cm", cm)//
				.addReference("oem", oem)//
				.addReference("httpBridgeFactory", factory)//
				.activate(config);

		final var apiBuilder = new JsonApiBuilder();
		sut.buildJsonApiRoutes(apiBuilder);

		final var params = JsonParser.parseString(GEOCODE_JSON_REQUEST_PARAMS).getAsJsonObject();
		final var request = new GenericJsonrpcRequest("geocode", params);
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(request);
		call.put(EdgeKeys.USER_KEY, DummyUser.DUMMY_OWNER);
		apiBuilder.handle(call);

		final var response = call.getResponse().toJsonObject();
		final var geoResults = GeoResult.serializer().toListSerializer()
				.deserialize(response.getAsJsonObject("result").get("geocodingResults"));

		assertEquals(geoResults.size(), 2);

		final var res1 = geoResults.get(0);
		assertEquals("Deutschland", res1.country());
		assertEquals(CountryCode.DE, res1.countryCode());
		assertEquals("Berlin", res1.subdivision());
		assertEquals(SubdivisionCode.DE_BE, res1.subdivisionCode());
		assertEquals("Mitte", res1.placeName());
		assertEquals("10115", res1.postcode());
		assertEquals("Invalidenstraße", res1.road());
		assertEquals("117", res1.houseNumber());
		assertEquals(52.5302814, res1.latitude(), 1e-6);
		assertEquals(13.385162, res1.longitude(), 1e-6);
		assertEquals("Europe/Berlin", res1.timezone().toString());
		assertEquals(Currency.EUR, res1.currency());
		assertEquals("https://www.openstreetmap.org/?mlat=52.53028&mlon=13.38516#map=17/52.53028/13.38516",
				res1.openStreetMapUrl());

		final var res2 = geoResults.get(1);
		assertEquals("Deutschland", res2.country());
		assertEquals(CountryCode.DE, res2.countryCode());
		assertEquals(null, res2.subdivision());
		assertEquals(SubdivisionCode.UNDEFINED, res2.subdivisionCode());
		assertEquals(null, res2.placeName());
		assertEquals(null, res2.postcode());
		assertEquals(null, res2.road());
		assertEquals(null, res2.houseNumber());
		assertEquals(52.5309086, res2.latitude(), 1e-6);
		assertEquals(13.3850058, res2.longitude(), 1e-6);
		assertEquals(null, res2.timezone());
		assertEquals(Currency.UNDEFINED, res2.currency());
		assertEquals(null, res2.openStreetMapUrl());
	}

	@Test
	public void testGeocode_ShouldFail_WhenLocationIsNullOrEmpty() {
		final var sut = new OpenCageGeocodingService(null, "TEST-API-KEY");
		final var nullFuture = sut.geocode(null);
		final var emptyFuture = sut.geocode("");
		final var blankFuture = sut.geocode("   ");

		var exNull = assertThrows(ExecutionException.class, nullFuture::get);
		assertTrue(exNull.getCause() instanceof IllegalArgumentException);

		var exEmpty = assertThrows(ExecutionException.class, emptyFuture::get);
		assertTrue(exEmpty.getCause() instanceof IllegalArgumentException);

		var exBlank = assertThrows(ExecutionException.class, blankFuture::get);
		assertTrue(exBlank.getCause() instanceof IllegalArgumentException);
	}

	@Test
	public void testGeocode_ShouldFail_WhenApiKeyIsNullOrEmpty() {
		final var nullSut = new OpenCageGeocodingService(null, null);
		var nullKeyFuture = nullSut.geocode("SomeLocation");
		var exNull = assertThrows(ExecutionException.class, nullKeyFuture::get);
		assertTrue(exNull.getCause() instanceof IllegalArgumentException);

		final var emptySut = new OpenCageGeocodingService(null, "");
		var emptyKeyFuture = emptySut.geocode("SomeLocation");
		var exEmpty = assertThrows(ExecutionException.class, emptyKeyFuture::get);
		assertTrue(exEmpty.getCause() instanceof IllegalArgumentException);

		final var blankSut = new OpenCageGeocodingService(null, "   ");
		var blankKeyFuture = blankSut.geocode("SomeLocation");
		var exBlank = assertThrows(ExecutionException.class, blankKeyFuture::get);
		assertTrue(exBlank.getCause() instanceof IllegalArgumentException);
	}

	@Test
	public void testGeocode_ShouldBuildCorrectUrl() {
		final var testApiKey = "TEST-API-KEY";
		final var testLocationQuery = "Berlin Mitte, Invalidenstraße 117";

		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			final String expectedBaseUrl = "https://api.opencagedata.com/geocode/v1/json";
			final String expectedLocationParam = "q="
					+ URLEncoder.encode(testLocationQuery, StandardCharsets.UTF_8).replace("+", "%20");
			final String expectedApiKeyParam = "key=" + testApiKey;
			final String expectedNoAnnotationsParam = "no_annotations=0";
			final String expectedNoRecordParam = "no_record=1";

			final var url = t.url();

			assertTrue("Base URL is incorrect", url.contains(expectedBaseUrl));
			assertTrue("Location param is missing or incorrect", url.contains(expectedLocationParam));
			assertTrue("API key param is missing or incorrect", url.contains(expectedApiKeyParam));
			assertTrue("No annotations param is missing or incorrect", url.contains(expectedNoAnnotationsParam));
			assertTrue("No record param is missing or incorrect", url.contains(expectedNoRecordParam));

			return HttpResponse.ok(null);
		});

		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				DummyBridgeHttpFactory::cycleSubscriber, //
				() -> fetcher, //
				() -> executor//
		);

		var httpBridge = factory.get();
		var geocodingService = new OpenCageGeocodingService(httpBridge, testApiKey);

		geocodingService.geocode(testLocationQuery);
	}

	@Test
	public void testGeoResult_ShouldDeserializeFromOpenCageApiJson() throws Exception {
		final var deserializer = GeoResult.fromOpenCageApiJsonDeserializer();
		final var result = deserializer.deserialize(OPEN_CAGE_API_RESPONSE_SINGLE_RESULT);

		assertNotNull(result);
		assertEquals("Deutschland", result.country());
		assertEquals(CountryCode.DE, result.countryCode());
		assertEquals("Berlin", result.subdivision());
		assertEquals(SubdivisionCode.DE_BE, result.subdivisionCode());
		assertEquals("Mitte", result.placeName());
		assertEquals("10115", result.postcode());
		assertEquals("Invalidenstraße", result.road());
		assertEquals("117", result.houseNumber());
		assertEquals(52.5302814, result.latitude(), 1e-6);
		assertEquals(13.385162, result.longitude(), 1e-6);
		assertEquals(ZoneId.of("Europe/Berlin"), result.timezone());
		assertEquals(Currency.EUR, result.currency());
		assertEquals("https://www.openstreetmap.org/?mlat=52.53028&mlon=13.38516#map=17/52.53028/13.38516",
				result.openStreetMapUrl());
	}

	@Test
	public void testGeoResult_ShouldSerializeAndDeserializeCorrectly() throws Exception {
		final var original = new GeoResult(//
				"Deutschland", //
				CountryCode.DE, //
				"Berlin", //
				SubdivisionCode.DE_BE, //
				"Mitte", //
				"10115", //
				"Invalidenstraße", //
				"117", //
				52.5302814, //
				13.385162, //
				ZoneId.of("Europe/Berlin"), //
				Currency.EUR, //
				"https://www.openstreetmap.org/?mlat=52.53028&mlon=13.38516#map=16/52.53028/13.38516");

		final var serializer = GeoResult.serializer();
		final var json = serializer.serialize(original).toString();

		final var deserialized = serializer.deserialize(json);

		assertNotNull(deserialized);
		assertEquals(original.country(), deserialized.country());
		assertEquals(original.countryCode(), deserialized.countryCode());
		assertEquals(original.subdivision(), deserialized.subdivision());
		assertEquals(original.subdivisionCode(), deserialized.subdivisionCode());
		assertEquals(original.placeName(), deserialized.placeName());
		assertEquals(original.postcode(), deserialized.postcode());
		assertEquals(original.road(), deserialized.road());
		assertEquals(original.houseNumber(), deserialized.houseNumber());
		assertEquals(original.latitude(), deserialized.latitude(), 0.000001);
		assertEquals(original.longitude(), deserialized.longitude(), 0.000001);
		assertEquals(original.timezone(), deserialized.timezone());
		assertEquals(original.currency(), deserialized.currency());
		assertEquals(original.openStreetMapUrl(), deserialized.openStreetMapUrl());
	}

	private static final String GEOCODE_JSON_REQUEST_PARAMS = """
			{
			  "query": "Berlin Mitte, Invalidenstraße 117"
			}
			""".stripIndent();

	private static final String OPEN_CAGE_API_RESPONSE = """
			{
			  "documentation": "https://opencagedata.com/api",
			  "licenses": [
			    {
			      "name": "see attribution guide",
			      "url": "https://opencagedata.com/credits"
			    }
			  ],
			  "rate": {
			    "limit": 100000,
			    "remaining": 96938,
			    "reset": 1745668800
			  },
			  "results": [
			    {
			      "annotations": {
			        "DMS": {
			          "lat": "52° 31' 49.01304'' N",
			          "lng": "13° 23' 6.58320'' E"
			        },
			        "MGRS": "33UUU8394721245",
			        "Maidenhead": "JO63gb67fg",
			        "Mercator": {
			          "x": 1490029.418,
			          "y": 6862645.128
			        },
			        "NUTS": {
			          "NUTS0": { "code": "DE" },
			          "NUTS1": { "code": "DE3" },
			          "NUTS2": { "code": "DE30" },
			          "NUTS3": { "code": "DE300" }
			        },
			        "OSM": {
			          "edit_url": "https://www.openstreetmap.org/edit?node=1731268880#map=17/52.53028/13.38516",
			          "note_url": "https://www.openstreetmap.org/note/new#map=17/52.53028/13.38516&layers=N",
			          "url": "https://www.openstreetmap.org/?mlat=52.53028&mlon=13.38516#map=17/52.53028/13.38516"
			        },
			        "UN_M49": {
			          "regions": {
			            "DE": "276",
			            "EUROPE": "150",
			            "WESTERN_EUROPE": "155",
			            "WORLD": "001"
			          },
			          "statistical_groupings": ["MEDC"]
			        },
			        "callingcode": 49,
			        "currency": {
			          "alternate_symbols": [],
			          "decimal_mark": ",",
			          "html_entity": "€",
			          "iso_code": "EUR",
			          "iso_numeric": "978",
			          "name": "Euro",
			          "smallest_denomination": 1,
			          "subunit": "Cent",
			          "subunit_to_unit": 100,
			          "symbol": "€",
			          "symbol_first": 0,
			          "thousands_separator": "."
			        },
			        "flag": "🇩🇪",
			        "geohash": "u33dbdfg0fhnmb15vcuf",
			        "qibla": 136.67,
			        "roadinfo": {
			          "drive_on": "right",
			          "road": "Invalidenstraße",
			          "speed_in": "km/h"
			        },
			        "sun": {
			          "rise": {
			            "apparent": 1750041840,
			            "astronomical": 0,
			            "civil": 1750038840,
			            "nautical": 1750033920
			          },
			          "set": {
			            "apparent": 1750102260,
			            "astronomical": 0,
			            "civil": 1750105260,
			            "nautical": 1750110180
			          }
			        },
			        "timezone": {
			          "name": "Europe/Berlin",
			          "now_in_dst": 1,
			          "offset_sec": 7200,
			          "offset_string": "+0200",
			          "short_name": "CEST"
			        },
			        "what3words": {
			          "words": "eisenbahn.scherz.anliegen"
			        },
			        "wikidata": "Q1254654"
			      },
			      "bounds": {
			        "northeast": {
			          "lat": 52.5303314,
			          "lng": 13.385212
			        },
			        "southwest": {
			          "lat": 52.5302314,
			          "lng": 13.385112
			        }
			      },
			      "components": {
			        "ISO_3166-1_alpha-2": "DE",
			        "ISO_3166-1_alpha-3": "DEU",
			        "ISO_3166-2": ["DE-BE"],
			        "_category": "commerce",
			        "_normalized_city": "Berlin",
			        "_type": "bar",
			        "bar": "Kunstfabrik Schlot",
			        "borough": "Mitte",
			        "city": "Berlin",
			        "continent": "Europe",
			        "country": "Deutschland",
			        "country_code": "de",
			        "house_number": "117",
			        "political_union": "European Union",
			        "postcode": "10115",
			        "road": "Invalidenstraße",
			        "state": "Berlin",
			        "state_code": "BE",
			        "suburb": "Mitte"
			      },
			      "confidence": 9,
			      "formatted": "Kunstfabrik Schlot, Invalidenstraße 117, 10115 Berlin, Deutschland",
			      "geometry": {
			        "lat": 52.5302814,
			        "lng": 13.385162
			      }
			    },
			    {
			      "components": {
			        "ISO_3166-1_alpha-2": "DE",
			        "ISO_3166-1_alpha-3": "DEU",
			        "country": "Deutschland",
			        "country_code": "de",
			        "political_union": "European Union"
			      },
			      "confidence": 10,
			      "formatted": "Berlin, Deutschland",
			      "geometry": {
			        "lat": 52.5309086,
			        "lng": 13.3850058
			      }
			    }
			  ],
			  "status": {
			    "code": 200,
			    "message": "OK"
			  },
			  "stay_informed": {
			    "blog": "https://blog.opencagedata.com",
			    "mastodon": "https://en.osm.town/@opencage"
			  },
			  "thanks": "For using an OpenCage API",
			  "timestamp": {
			    "created_http": "Mon, 16 Jun 2025 07:15:06 GMT",
			    "created_unix": 1750058106
			  },
			  "total_results": 2
			}
			""".stripIndent();

	private static final String OPEN_CAGE_API_RESPONSE_SINGLE_RESULT = """
			{
			  "annotations": {
			    "DMS": {
			      "lat": "52° 31' 49.01304'' N",
			      "lng": "13° 23' 6.58320'' E"
			    },
			    "MGRS": "33UUU9045821245",
			    "Maidenhead": "JO62qm67fg",
			    "Mercator": {
			      "x": 1490029.418,
			      "y": 6862645.128
			    },
			    "NUTS": {
			      "NUTS0": { "code": "DE" },
			      "NUTS1": { "code": "DE3" },
			      "NUTS2": { "code": "DE30" },
			      "NUTS3": { "code": "DE300" }
			    },
			    "OSM": {
			      "edit_url": "https://www.openstreetmap.org/edit?node=1731268880#map=17/52.53028/13.38516",
			      "note_url": "https://www.openstreetmap.org/note/new#map=17/52.53028/13.38516&layers=N",
			      "url": "https://www.openstreetmap.org/?mlat=52.53028&mlon=13.38516#map=17/52.53028/13.38516"
			    },
			    "UN_M49": {
			      "regions": {
			        "DE": "276",
			        "EUROPE": "150",
			        "WESTERN_EUROPE": "155",
			        "WORLD": "001"
			      },
			      "statistical_groupings": ["MEDC"]
			    },
			    "callingcode": 49,
			    "currency": {
			      "alternate_symbols": [],
			      "decimal_mark": ",",
			      "html_entity": "€",
			      "iso_code": "EUR",
			      "iso_numeric": "978",
			      "name": "Euro",
			      "smallest_denomination": 1,
			      "subunit": "Cent",
			      "subunit_to_unit": 100,
			      "symbol": "€",
			      "symbol_first": 0,
			      "thousands_separator": "."
			    },
			    "flag": "🇩🇪",
			    "geohash": "u33dbdfg0fhnmb15vcuf",
			    "qibla": 136.67,
			    "roadinfo": {
			      "drive_on": "right",
			      "road": "Invalidenstraße",
			      "speed_in": "km/h"
			    },
			    "sun": {
			      "rise": {
			        "apparent": 1750041840,
			        "astronomical": 0,
			        "civil": 1750038840,
			        "nautical": 1750033920
			      },
			      "set": {
			        "apparent": 1750102260,
			        "astronomical": 0,
			        "civil": 1750105260,
			        "nautical": 1750110180
			      }
			    },
			    "timezone": {
			      "name": "Europe/Berlin",
			      "now_in_dst": 1,
			      "offset_sec": 7200,
			      "offset_string": "+0200",
			      "short_name": "CEST"
			    },
			    "what3words": {
			      "words": "eisenbahn.scherz.anliegen"
			    },
			    "wikidata": "Q1254654"
			  },
			  "bounds": {
			    "northeast": {
			      "lat": 52.5303314,
			      "lng": 13.385212
			    },
			    "southwest": {
			      "lat": 52.5302314,
			      "lng": 13.385112
			    }
			  },
			  "components": {
			    "ISO_3166-1_alpha-2": "DE",
			    "ISO_3166-1_alpha-3": "DEU",
			    "ISO_3166-2": ["DE-BE"],
			    "_category": "commerce",
			    "_normalized_city": "Berlin",
			    "_type": "bar",
			    "bar": "Kunstfabrik Schlot",
			    "borough": "Mitte",
			    "city": "Berlin",
			    "continent": "Europe",
			    "country": "Deutschland",
			    "country_code": "de",
			    "house_number": "117",
			    "political_union": "European Union",
			    "postcode": "10115",
			    "road": "Invalidenstraße",
			    "state": "Berlin",
			    "state_code": "BE",
			    "suburb": "Mitte"
			  },
			  "confidence": 9,
			  "formatted": "Kunstfabrik Schlot, Invalidenstraße 117, 10115 Berlin, Deutschland",
			  "geometry": {
			    "lat": 52.5302814,
			    "lng": 13.385162
			  }
			}
			""".stripIndent();
}
