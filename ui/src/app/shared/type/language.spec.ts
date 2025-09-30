// @ts-strict-ignore
import { TestContext, TestingUtils } from "../components/shared/testing/UTILS.SPEC";
import { Language } from "./language";
describe("Language", () => {

  let TEST_CONTEXT: TestContext;
  beforeAll(async () => {
    TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP();
  });

  it("#geti18nLocaleByKey", () => {
    expect(LANGUAGE.GETI18N_LOCALE_BY_KEY("DE")).toBe("de");
    expect(LANGUAGE.GETI18N_LOCALE_BY_KEY("Zz")).toBe(LANGUAGE.DEFAULT.I18N_LOCALE_KEY);
    expect(LANGUAGE.GETI18N_LOCALE_BY_KEY(null)).toBe(LANGUAGE.DEFAULT.I18N_LOCALE_KEY);
    expect(LANGUAGE.GETI18N_LOCALE_BY_KEY(undefined)).toBe(LANGUAGE.DEFAULT.I18N_LOCALE_KEY);
  });

  it("#setAdditionalTranslationFile - translation de found", async () => {
    const json = {
      "de": {
        "key": "value",
      },
    };
    TEST_CONTEXT.TRANSLATE.USE("de");

    expect(await LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(json, TEST_CONTEXT.translate)).toEqual({ lang: "de", translations: Object({ key: "value" }), shouldMerge: true });
  });

  it("#setAdditionalTranslationFile - translation de not found - warning expected", async () => {
    const json = {
      "de": {
        "key": "value",
      },
    };

    spyOn(console, "warn");
    TEST_CONTEXT.TRANSLATE.CURRENT_LANG = "cz";
    await LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(json, TEST_CONTEXT.translate);
    expect(CONSOLE.WARN).toHaveBeenCalledWith("No translation available for Language cz. Implemented languages are: de");
  });
});
