import { environment } from "src/environments";
import { Service } from "../../service/service";
import { Language } from "../../type/language";

/** Manages links to documentation pages */
export class DocsUtils {
    public static createDataProtectionLink(service: Service) {
        const link = environment.links.DATA_PROTECTION;

        if (link == null) {
            return null;
        }
        return link.replace("{language}", service.getDocsLang());
    }

    /**
     * Replaces the {language} placeholder in a docs url with the given docs language.
     *
     * @param url The url containing a {language} placeholder
     * @param lang The docs language, e.g. from {@link Service#getDocsLang}
     * @returns The url with the placeholder replaced, or null if url is not set
     */
    public static replaceDocsLanguage(url: string | null | undefined, lang: string): string | null {
        if (url == null || url === "") {
            return null;
        }

        return url.replace("{language}", lang);
    }

    /**
     * Replaces the {language} placeholder in a docs url with the docs language of the current UI language.
     *
     * Use this where no {@link Service} is available, otherwise prefer {@link DocsUtils.replaceDocsLanguage} with
     * {@link Service#getDocsLang}.
     *
     * @param url The url containing a {language} placeholder
     * @returns The url with the placeholder replaced, or an empty string if url is not set
     */
    public static replaceWithCurrentDocsLanguage(url: string | null | undefined): string {
        return DocsUtils.replaceDocsLanguage(url, Language.getDocsLang(Language.getCurrentLanguage().key)) ?? "";
    }
}
