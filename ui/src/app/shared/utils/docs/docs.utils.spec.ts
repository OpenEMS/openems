import { Language } from "../../type/language";
import { DocsUtils } from "./docs.utils";

describe("DocsUtils", () => {
    const URL = "https://docs.fenecon.de/redirect/fenecon/{language}/home_10_manual";

    describe("+replaceWithCurrentDocsLanguage", () => {
        let storedLanguage: string | null;

        beforeEach(() => {
            storedLanguage = localStorage.getItem("LANGUAGE");
        });

        afterEach(() => {
            if (storedLanguage == null) {
                localStorage.removeItem("LANGUAGE");
            } else {
                localStorage.setItem("LANGUAGE", storedLanguage);
            }
        });

        it("should use german docs for german", () => {
            localStorage.setItem("LANGUAGE", Language.DE.key);
            expect(DocsUtils.replaceWithCurrentDocsLanguage(URL)).toBe(
                "https://docs.fenecon.de/redirect/fenecon/de/home_10_manual",
            );
        });

        it("should fall back to english docs for other languages", () => {
            localStorage.setItem("LANGUAGE", Language.NL.key);
            expect(DocsUtils.replaceWithCurrentDocsLanguage(URL)).toBe(
                "https://docs.fenecon.de/redirect/fenecon/en/home_10_manual",
            );
        });

        it("should return an empty string if url is not set", () => {
            expect(DocsUtils.replaceWithCurrentDocsLanguage(null)).toBe("");
            expect(DocsUtils.replaceWithCurrentDocsLanguage(undefined)).toBe("");
        });
    });
});
