import { ALL_COUNTRIES, Country, CountryUtils, DEFAULT_COUNTRY } from "./country";
describe("Country", () => {

    it("#ALL_COUNTRIES", () => {
        expect(ALL_COUNTRIES).toEqual([
            Country.GERMANY, Country.AUSTRIA, Country.SWITZERLAND, Country.SWEDEN,
            Country.CZECH_REPUBLIC, Country.NETHERLANDS, Country.GREECE, Country.LITHUANIA,
        ]);
    });

    describe("CountryUtils", () => {

        it("#fromCountryCode_null/undefined/empty_inputs", () => {
            expect(CountryUtils.fromCountryCode(null)).toBe(DEFAULT_COUNTRY);
            expect(CountryUtils.fromCountryCode(undefined)).toBe(DEFAULT_COUNTRY);
            expect(CountryUtils.fromCountryCode("" as unknown as string)).toBe(DEFAULT_COUNTRY);
            expect(CountryUtils.fromCountryCode("   " as unknown as string)).toBe(DEFAULT_COUNTRY);
        });

        it("#fromCountryCode_case-insensitive_and_trim_input", () => {
            expect(CountryUtils.fromCountryCode("DE")).toBe(Country.GERMANY);
            expect(CountryUtils.fromCountryCode(" de ")).toBe(Country.GERMANY);
            expect(CountryUtils.fromCountryCode("At")).toBe(Country.AUSTRIA);
            expect(CountryUtils.fromCountryCode("CH")).toBe(Country.SWITZERLAND);
            expect(CountryUtils.fromCountryCode("Se")).toBe(Country.SWEDEN);
            expect(CountryUtils.fromCountryCode("NL")).toBe(Country.NETHERLANDS);
            expect(CountryUtils.fromCountryCode("CZ")).toBe(Country.CZECH_REPUBLIC);
            expect(CountryUtils.fromCountryCode("GR")).toBe(Country.GREECE);
            expect(CountryUtils.fromCountryCode("LT")).toBe(Country.LITHUANIA);
        });

        it("#fromCountryCode_DEFAULT_COUNTRY_for_invalidCodes", () => {
            expect(CountryUtils.fromCountryCode("xx")).toBe(DEFAULT_COUNTRY);
            expect(CountryUtils.fromCountryCode("invalid")).toBe(DEFAULT_COUNTRY);
        });
    });
});
