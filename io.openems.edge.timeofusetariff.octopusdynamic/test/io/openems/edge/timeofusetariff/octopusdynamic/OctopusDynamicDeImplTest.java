package io.openems.edge.timeofusetariff.octopusdynamic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

import com.google.gson.JsonParser;

/**
 * Offline contract tests for Octopus DE quarter-hour prices. No real account
 * data, network access or credentials are required.
 */
public class OctopusDynamicDeImplTest {

    private static String forecast(String slots) {
        return """
                {"account":{"properties":[{"electricityMalos":[{"agreements":[{
                  "unitRateForecast":[%s]
                }]}]}]}}
                """.formatted(slots);
    }

    private static String slot(String from, String to, String price) {
        return """
                {"validFrom":"%s","validTo":"%s",
                 "unitRateInformation":{"rates":[{"latestGrossUnitRateCentsPerKwh":%s}]}}
                """.formatted(from, to, price);
    }

    private static io.openems.edge.timeofusetariff.api.TimeOfUsePrices parse(String json) throws IOException {
        return OctopusDynamicDeImpl.parsePrices(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void convertsGrossCentPerKwhToEurPerMwh() throws Exception {
        var first = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
        var second = slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "18.25");
        assertArrayEquals(new Double[]{215.0, 182.5}, parse(forecast(first + "," + second)).asArray());
    }

    @Test
    public void rejectsMissingQuarter() {
        var first = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
        var third = slot("2026-10-11T10:30:00Z", "2026-10-11T10:45:00Z", "18.25");
        assertThrows(IOException.class, () -> parse(forecast(first + "," + third)));
    }

    @Test
    public void rejectsInvalidSlotDuration() {
        var broken = slot("2026-10-11T10:00:00Z", "2026-10-11T11:00:00Z", "21.5");
        assertThrows(IOException.class, () -> parse(forecast(broken)));
    }

    @Test
    public void rejectsAmbiguousAgreements() {
        var a = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
        var b = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "27.0");
        assertThrows(IOException.class, () -> parse(forecast(a + "," + b)));
    }

    @Test
    public void rejectsEmptyOrMalformedData() {
        assertThrows(IOException.class, () -> parse(forecast("")));
        assertThrows(IOException.class, () -> parse("{}"));
    }

    @Test
    public void handlesUtcTimestampsAcrossDaylightSavingTime() throws Exception {
        var a = slot("2026-10-25T00:45:00Z", "2026-10-25T01:00:00Z", "10");
        var b = slot("2026-10-25T01:00:00Z", "2026-10-25T01:15:00Z", "11");
        assertArrayEquals(new Double[]{100.0, 110.0}, parse(forecast(a + "," + b)).asArray());
    }
}
