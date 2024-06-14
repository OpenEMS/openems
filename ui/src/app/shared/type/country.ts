import { TranslateService } from "@ngx-translate/core";

export enum Country {
    GERMANY = 'de',
    AUSTRIA = 'at',
    SWITZERLAND = 'ch',
    SWEDEN = 'se',
    CZECH_REPUBLIK = 'cz',
    NETHERLANDS = 'nl',
}

export const COUNTRY_OPTIONS = (translate: TranslateService) => {
    return [
        { value: Country.GERMANY, label: translate.instant('GENERAL.COUNTRY.GERMANY') },
        { value: Country.AUSTRIA, label: translate.instant('GENERAL.COUNTRY.AUSTRIA') },
        { value: Country.SWITZERLAND, label: translate.instant('GENERAL.COUNTRY.SWITZERLAND') },
        { value: Country.SWEDEN, label: translate.instant('GENERAL.COUNTRY.SWEDEN') },
        { value: Country.NETHERLANDS, label: translate.instant('GENERAL.COUNTRY.NETHERLANDS') },
        { value: Country.CZECH_REPUBLIK, label: translate.instant('GENERAL.COUNTRY.CZECH') },
    ];
};
