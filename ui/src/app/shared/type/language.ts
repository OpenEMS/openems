import localCS from "@angular/common/locales/cs";
import localDE from "@angular/common/locales/de";
import localEN from "@angular/common/locales/en";
import localES from "@angular/common/locales/es";
import localFR from "@angular/common/locales/fr";
import localJA from "@angular/common/locales/ja";
import localNL from "@angular/common/locales/nl";
import { TranslateLoader } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import cz from "src/assets/i18n/cz.json";
import de from "src/assets/i18n/de.json";
import en from "src/assets/i18n/en.json";
import es from "src/assets/i18n/es.json";
import fr from "src/assets/i18n/fr.json";
import ja from "src/assets/i18n/ja.json";
import nl from "src/assets/i18n/nl.json";
import { environment } from "src/environments";

export interface Translation {
    [key: string]: string | Translation;
}

export class MyTranslateLoader implements TranslateLoader {

    public getTranslation(key: string): Observable<Translation> {
        const language = Language.getByKey(key);
        if (language) {
            return of(language.json);
        }
        return of(Language.DEFAULT.json);
    }
}
export type LanguageKeyUnion = (typeof Language.ALL)[number]["key"];

export class Language {

    public static readonly DE: Language = new Language("German", "de", "de", de, localDE);
    public static readonly EN: Language = new Language("English", "en", "en", en, localEN);
    public static readonly CS: Language = new Language("Czech", "cs", "de", cz, localCS /* NOTE: there is no locale in @angular/common for Czech */);
    public static readonly NL: Language = new Language("Dutch", "nl", "nl", nl, localNL);
    public static readonly ES: Language = new Language("Spanish", "es", "es", es, localES);
    public static readonly FR: Language = new Language("French", "fr", "fr", fr, localFR);
    public static readonly JA: Language = new Language("Japanese", "ja", "ja", ja, localJA);

    public static readonly ALL = [Language.DE, Language.EN, Language.CS, Language.NL, Language.ES, Language.FR, Language.JA];
    public static readonly DEFAULT = Language.getByKey(environment.defaultLanguage) as Language;

    constructor(
        public readonly title: string,
        public readonly key: string,
        public readonly i18nLocaleKey: string,
        public readonly json: any,
        // Angular is not providing common type for locale.
        // https://github.com/angular/angular/issues/30506

        public readonly locale: any,
    ) {
    }

    public static get SYSTEM(): Language | null {
        return Language.getByBrowserLang(navigator.language || navigator["userLanguage"]);
    }

    public static get LOCAL_STORAGE(): Language | null {
        return Language.getByKey(localStorage.LANGUAGE);
    }


    public static getByKey(key: string): Language | null {
        for (const language of Language.ALL) {

            if (language.key == key) {
                return language;
            }
        }
        return null;
    }

    public static getByBrowserLang(browserLang: string): Language | null {
        switch (browserLang) {
            case "de": return Language.DE;
            case "en":
            case "en-US":
                return Language.EN;
            case "es": return Language.ES;
            case "nl": return Language.NL;
            case "cs": return Language.CS;
            case "fr": return Language.FR;
            case "ja": return Language.JA;
            default: return null;
        }
    }

    public static getLocale(language: string) {
        switch (language) {
            case Language.DE.key: return Language.DE.locale;
            case Language.EN.key: return Language.EN.locale;
            case Language.ES.key: return Language.ES.locale;
            case Language.NL.key: return Language.NL.locale;
            case Language.CS.key: return Language.CS.locale;
            case Language.FR.key: return Language.FR.locale;
            case Language.JA.key: return Language.JA.locale;
            default: return Language.DEFAULT.locale;
        }
    }

    /**
     * Gets the i18n locale with passed key
     *
     * @param language the language
     * @returns the i18n locale
     */
    public static geti18nLocaleByKey(language: string) {
        const lang = this.getByBrowserLang(language?.toLowerCase());

        if (!lang) {
            console.warn(`Key ${language} not part of ${Language.ALL.map(lang => lang.title + ":" + lang.key)}`);
        }

        return lang?.i18nLocaleKey ?? Language.DEFAULT.i18nLocaleKey;
    }

    /**
     * Gets the i18n locale key without passed key
     *
     * @returns the i18n locale key
     */
    public static geti18nLocale() {
        return Language.getCurrentLanguage().i18nLocaleKey;
    }

    /**
     * Sets a additional translation file
     *
     * e.g. AdvertismentModule
     *
     *  IMPORTANT: Translation keys will overwrite each other.
     *  Make sure to use a unique top level key.
     *
     * @param translations the translation files
     * @returns translations params
     */
    public static async normalizeAdditionalTranslationFiles(translations: Record<LanguageKeyUnion, any>) {
        return Object.entries(translations).map(([key, value]) => ({ lang: key, translation: value, shouldMerge: true }));
    }


    /**
     * Gets the i18n locale key without passed key
     *
     * @returns the i18n locale key
     */
    public static getCurrentLanguage() {
        return Language.LOCAL_STORAGE ?? Language.SYSTEM ?? Language.DEFAULT;
    }
}
