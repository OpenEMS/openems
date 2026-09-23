import { TestContext, TestingUtils } from "../components/shared/testing/utils.spec";
import { Language } from "./language";
describe("Language", () => {
    let TEST_CONTEXT: TestContext;
    beforeAll(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });

    it("#geti18nLocaleByKey", () => {
        const warnSpy = spyOn(console, "warn").and.stub();

        expect(Language.geti18nLocaleByKey("de")).toBe("de");
        expect(Language.geti18nLocaleByKey("Zz")).toBe(Language.DEFAULT.i18nLocaleKey);
        expect(Language.geti18nLocaleByKey(null)).toBe(Language.DEFAULT.i18nLocaleKey);
        expect(Language.geti18nLocaleByKey(undefined)).toBe(Language.DEFAULT.i18nLocaleKey);
        expect(warnSpy).toHaveBeenCalledTimes(3);
    });

    it("#setAdditionalTranslationFile - translation de found", async () => {
        const json = {
            KEY: "VALUE",
        };
        TEST_CONTEXT.translate.use("de");
        expect(await Language.normalizeAdditionalTranslationFiles({ de: json })).toEqual([
            { lang: "de", translation: { KEY: "VALUE" }, shouldMerge: true },
        ]);
    });
});
