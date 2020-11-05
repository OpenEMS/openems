import { TranslateLoader } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { TRANSLATION as CZ } from './cz';
import { TRANSLATION as DE } from './de';
import { TRANSLATION as EN } from './en';
import { TRANSLATION as ES } from './es';
import { TRANSLATION as NL } from './nl';
import { TRANSLATION as FR } from './fr';


export enum LanguageTag {
    EN = "English",
    DE = "German",
    CZ = "Czech",
    NL = "Dutch",
    ES = "Spanish",
    FR = "French"
}

export class Language implements TranslateLoader {

    public static getLanguageTags(): LanguageTag[] {
        return Object.keys(LanguageTag).map(key => LanguageTag[key]);
    }

    public static getLanguages(): string[] {
        return Object.keys(LanguageTag).map(key => key.toLowerCase());
    }

    constructor() { }

    getTranslation(lang: LanguageTag): Observable<any> {
        switch (lang) {
            case LanguageTag.DE:
                return of(DE);
            case LanguageTag.CZ:
                return of(CZ);
            case LanguageTag.NL:
                return of(NL);
            case LanguageTag.ES:
                return of(ES);
            case LanguageTag.EN:
                return of(EN);
            case LanguageTag.FR:
                return of(FR);
        }
        return of(EN);
    }
}
