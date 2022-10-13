export class Language {

    public static readonly DE: Language = new Language("German", "de");
    public static readonly EN: Language = new Language("English", "en");
    public static readonly CZ: Language = new Language("Czech", "cz");
    public static readonly NL: Language = new Language("Dutch", "nl");
    public static readonly ES: Language = new Language("Spanish", "es");
    public static readonly FR: Language = new Language("French", "fr");

    public static readonly ALL = [Language.DE, Language.EN, Language.CZ, Language.NL, Language.ES, Language.FR];
    public static readonly DEFAULT = Language.DE;

    public static getByFilename(filename: string): Language | null {
        for (let language of Language.ALL) {
            if (language.filename == filename) {
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
        public readonly filename: string
    ) {
    }
}