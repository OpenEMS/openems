import localCS from "@angular/common/locales/cs";
import localDE from "@angular/common/locales/de";
import localEN from "@angular/common/locales/en";
import localES from "@angular/common/locales/es";
import localFR from "@angular/common/locales/fr";
import localJA from "@angular/common/locales/ja";
import localNL from "@angular/common/locales/nl";
import { TranslateLoader, TranslateService } from "@ngx-translate/core";
import { filter, Observable, of, take } from "rxjs";
import cz from "src/assets/i18n/CZ.JSON";
import de from "src/assets/i18n/DE.JSON";
import en from "src/assets/i18n/EN.JSON";
import es from "src/assets/i18n/ES.JSON";
import fr from "src/assets/i18n/FR.JSON";
import ja from "src/assets/i18n/JA.JSON";
import nl from "src/assets/i18n/NL.JSON";
import { environment } from "src/environments";

export interface Translation {
    [key: string]: string | Translation;
}

export class MyTranslateLoader implements TranslateLoader {

    public getTranslation(key: string): Observable<Translation> {
        const language = LANGUAGE.GET_BY_KEY(key);
        if (language) {
            return of(LANGUAGE.JSON);
        }
        return of(LANGUAGE.DEFAULT.JSON);
    }
}

export class Language {

    public static readonly DE: Language = new Language("German", "de", "de", de, localDE);
    public static readonly EN: Language = new Language("English", "en", "en", en, localEN);
    public static readonly CS: Language = new Language("Czech", "cs", "de", cz, localCS /* NOTE: there is no locale in @angular/common for Czech */);
    public static readonly NL: Language = new Language("Dutch", "nl", "nl", nl, localNL);
    public static readonly ES: Language = new Language("Spanish", "es", "es", es, localES);
    public static readonly FR: Language = new Language("French", "fr", "fr", fr, localFR);
    public static readonly JA: Language = new Language("Japanese", "ja", "ja", ja, localJA);

    public static readonly ALL = [LANGUAGE.DE, LANGUAGE.EN, LANGUAGE.CS, LANGUAGE.NL, LANGUAGE.ES, LANGUAGE.FR, LANGUAGE.JA];
    public static readonly DEFAULT = LANGUAGE.GET_BY_KEY(ENVIRONMENT.DEFAULT_LANGUAGE) as Language;

    constructor(
        public readonly title: string,
        public readonly key: string,
        public readonly i18nLocaleKey: string,
        public readonly json: any,
        // Angular is not providing common type for locale.
        // https://GITHUB.COM/angular/angular/issues/30506

        public readonly locale: any,
    ) {
    }

    public static getByKey(key: string): Language | null {
        for (const language of LANGUAGE.ALL) {

            if (LANGUAGE.KEY == key) {
                return language;
            }
        }
        return null;
    }

    public static getByBrowserLang(browserLang: string): Language | null {
        switch (browserLang) {
            case "de": return LANGUAGE.DE;
            case "en":
            case "en-US":
                return LANGUAGE.EN;
            case "es": return LANGUAGE.ES;
            case "nl": return LANGUAGE.NL;
            case "cs": return LANGUAGE.CS;
            case "fr": return LANGUAGE.FR;
            case "ja": return LANGUAGE.JA;
            default: return null;
        }
    }

    public static getLocale(language: string) {
        switch (language) {
            case LANGUAGE.DE.KEY: return LANGUAGE.DE.LOCALE;
            case LANGUAGE.EN.KEY: return LANGUAGE.EN.LOCALE;
            case LANGUAGE.ES.KEY: return LANGUAGE.ES.LOCALE;
            case LANGUAGE.NL.KEY: return LANGUAGE.NL.LOCALE;
            case LANGUAGE.CS.KEY: return LANGUAGE.CS.LOCALE;
            case LANGUAGE.FR.KEY: return LANGUAGE.FR.LOCALE;
            case LANGUAGE.JA.KEY: return LANGUAGE.JA.LOCALE;
            default: return LANGUAGE.DEFAULT.LOCALE;
        }
    }

    /**
     * Gets the i18n locale with passed key
     *
     * @param language the language
     * @returns the i18n locale
     */
    public static geti18nLocaleByKey(language: string) {
        const lang = THIS.GET_BY_BROWSER_LANG(language?.toLowerCase());

        if (!lang) {
            CONSOLE.WARN(`Key ${language} not part of ${LANGUAGE.ALL.MAP(lang => LANG.TITLE + ":" + LANG.KEY)}`);
        }

        return lang?.i18nLocaleKey ?? LANGUAGE.DEFAULT.I18N_LOCALE_KEY;
    }

    /**
     * Sets a additional translation file
     *
     * E.G. AdvertismentModule
     *
     *  IMPORTANT: Translation keys will overwrite each other.
     *  Make sure to use a unique top level key.
     *
     * @param translationFile the translation file
     * @returns translations params
     */
    public static async setAdditionalTranslationFile(translationFile: any, translate: TranslateService): Promise<{ lang: string; translations: {}; shouldMerge?: boolean; }> {
        const lang = TRANSLATE.CURRENT_LANG ?? (await TRANSLATE.ON_LANG_CHANGE.PIPE(filter(lang => !!lang), take(1)).toPromise())?.lang ?? LANGUAGE.DEFAULT.KEY;
        let translationKey: string = lang;

        if (!(LANGUAGE.DEFAULT.KEY in translationFile)) {
            throw new Error(`Translation for fallback ${LANGUAGE.DEFAULT.KEY} is missing`);
        }

        if (!(lang in translationFile)) {

            if (ENVIRONMENT.DEBUG_MODE) {
                CONSOLE.WARN(`No translation available for Language ${lang}. Implemented languages are: ${OBJECT.KEYS(translationFile)}`);
            }
            translationKey = LANGUAGE.EN.KEY;
        }
        return { lang: lang, translations: translationFile[translationKey], shouldMerge: true };
    }
}
