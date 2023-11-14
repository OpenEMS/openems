import { Country } from "src/app/shared/type/country";

export enum SafetyCountry {
    GERMANY = 'GERMANY',
    AUSTRIA = 'AUSTRIA',
    SWITZERLAND = 'SWITZERLAND',
    SWEDEN = 'SWEDEN',
    CZECH_REPUBLIK = 'CZECH',
    NETHERLANDS = 'HOLLAND',
}

export namespace SafetyCountry {
    export function getSafetyCountry(countryValue: Country) {
        switch (countryValue) {
            case Country.GERMANY:
                return SafetyCountry.GERMANY;
            case Country.AUSTRIA:
                return SafetyCountry.AUSTRIA;
            case Country.SWITZERLAND:
                return SafetyCountry.SWITZERLAND;
            case Country.CZECH_REPUBLIK:
                return SafetyCountry.CZECH_REPUBLIK;
            case Country.NETHERLANDS:
                return SafetyCountry.NETHERLANDS;
            case Country.SWEDEN:
                return SafetyCountry.SWEDEN;
        }
    }
}
