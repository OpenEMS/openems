import { TranslateLoader } from "@ngx-translate/core";
import { Observable, of } from 'rxjs';
import cz from 'src/assets/i18n/cz.json';
import de from 'src/assets/i18n/de.json';
import en from 'src/assets/i18n/en.json';
import es from 'src/assets/i18n/es.json';
import fr from 'src/assets/i18n/fr.json';
import nl from 'src/assets/i18n/nl.json';

export class MyTranslateLoader implements TranslateLoader {

    public getTranslation(key: string): Observable<any> {
        var language = Language.getByKey(key);
        if (language) {
            return of(language.json);
        }
        return of(Language.DEFAULT.json);
    }
}

export class Language {

    public static readonly DE: Language = new Language("German", "de", de);
    public static readonly EN: Language = new Language("English", "en", en);
    public static readonly CZ: Language = new Language("Czech", "cz", cz);
    public static readonly NL: Language = new Language("Dutch", "nl", nl);
    public static readonly ES: Language = new Language("Spanish", "es", es);
    public static readonly FR: Language = new Language("French", "fr", fr);

    public static readonly ALL = [Language.DE, Language.EN, Language.CZ, Language.NL, Language.ES, Language.FR];
    public static readonly DEFAULT = Language.EN;

    public static getByKey(key: string): Language | null {
        for (let language of Language.ALL) {
            if (language.key == key) {
                return language;
            }
        }
        return null;
    }

    public static getByBrowserLang(browserLang: string): Language | null {
        switch (browserLang) {
            case "de": return Language.DE;
            case "en": return Language.EN;
            case "es": return Language.ES;
            case "nl": return Language.NL;
            case "cz": return Language.CZ;
            case "fr": return Language.FR;
            default: return null;
        }
    }

    constructor(
        public readonly title: string,
        public readonly key: string,
        public readonly json: any
    ) {
    }
}
