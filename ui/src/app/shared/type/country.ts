import { TranslateService } from "@ngx-translate/core";

export enum Country {
    GERMANY = "de",
    AUSTRIA = "at",
    SWITZERLAND = "ch",
    SWEDEN = "se",
    CZECH_REPUBLIC = "cz",
    NETHERLANDS = "nl",
    GREECE = "gr",
    LITHUANIA = "lt",
}

export const DEFAULT_COUNTRY = Country.GERMANY;

export const ALL_COUNTRIES: Country[] = [
    Country.GERMANY,
    Country.AUSTRIA,
    Country.SWITZERLAND,
    Country.SWEDEN,
    Country.CZECH_REPUBLIC,
    Country.NETHERLANDS,
    Country.GREECE,
    Country.LITHUANIA,
];

export interface CountryOption {
    value: Country;
    label: string;
}

export const COUNTRY_OPTIONS = (translate: TranslateService) => {
    return [
        { value: Country.GERMANY, label: translate.instant("GENERAL.COUNTRY.GERMANY") },
        { value: Country.AUSTRIA, label: translate.instant("GENERAL.COUNTRY.AUSTRIA") },
        { value: Country.SWITZERLAND, label: translate.instant("GENERAL.COUNTRY.SWITZERLAND") },
        { value: Country.SWEDEN, label: translate.instant("GENERAL.COUNTRY.SWEDEN") },
        { value: Country.NETHERLANDS, label: translate.instant("GENERAL.COUNTRY.NETHERLANDS") },
        { value: Country.CZECH_REPUBLIC, label: translate.instant("GENERAL.COUNTRY.CZECH") },
        { value: Country.GREECE, label: translate.instant("GENERAL.COUNTRY.GREECE") },
        { value: Country.LITHUANIA, label: translate.instant("GENERAL.COUNTRY.LITHUANIA") },
    ];
};

export class CountryUtils {

    public static fromCountryCode(countryCode: string | null | undefined): Country {
        if (countryCode == null) {
            return DEFAULT_COUNTRY;
        }

        const normalizedCode = countryCode.trim().toLowerCase();

        // Use the enum values directly for validation
        return ALL_COUNTRIES.find(country => country === normalizedCode as Country) ?? DEFAULT_COUNTRY;
    }
}

