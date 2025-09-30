import { TranslateService } from "@ngx-translate/core";

export enum Country {
    GERMANY = "de",
    AUSTRIA = "at",
    SWITZERLAND = "ch",
    SWEDEN = "se",
    CZECH_REPUBLIK = "cz",
    NETHERLANDS = "nl",
    GREECE = "gr",
}

export const COUNTRY_OPTIONS = (translate: TranslateService) => {
    return [
        { value: COUNTRY.GERMANY, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.GERMANY") },
        { value: COUNTRY.AUSTRIA, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.AUSTRIA") },
        { value: COUNTRY.SWITZERLAND, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.SWITZERLAND") },
        { value: COUNTRY.SWEDEN, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.SWEDEN") },
        { value: COUNTRY.NETHERLANDS, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.NETHERLANDS") },
        { value: Country.CZECH_REPUBLIK, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.CZECH") },
        { value: COUNTRY.GREECE, label: TRANSLATE.INSTANT("GENERAL.COUNTRY.GREECE") },
    ];
};
