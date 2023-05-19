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
        { value: Country.GERMANY, label: translate.instant('General.Country.germany') },
        { value: Country.AUSTRIA, label: translate.instant('General.Country.austria') },
        { value: Country.SWITZERLAND, label: translate.instant('General.Country.switzerland') },
        { value: Country.SWEDEN, label: translate.instant('General.Country.sweden') },
        { value: Country.NETHERLANDS, label: translate.instant('General.Country.netherlands') },
        { value: Country.CZECH_REPUBLIK, label: translate.instant('General.Country.czech') },
    ];
};