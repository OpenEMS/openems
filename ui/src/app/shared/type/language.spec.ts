// @ts-strict-ignore
import { TestContext, TestingUtils } from "../components/shared/testing/utils.spec";
import { Language } from "./language";
describe("Language", () => {

  let TEST_CONTEXT: TestContext;
  beforeAll(async () => {
    TEST_CONTEXT = await TestingUtils.sharedSetup();
  });

  it("#geti18nLocaleByKey", () => {
    expect(Language.geti18nLocaleByKey("DE")).toBe("de");
    expect(Language.geti18nLocaleByKey("Zz")).toBe(Language.DEFAULT.i18nLocaleKey);
    expect(Language.geti18nLocaleByKey(null)).toBe(Language.DEFAULT.i18nLocaleKey);
    expect(Language.geti18nLocaleByKey(undefined)).toBe(Language.DEFAULT.i18nLocaleKey);
  });

  it("#setAdditionalTranslationFile - translation de found", async () => {
    const json = {
      "de": {
        "key": "value",
      },
    };
    TEST_CONTEXT.translate.use("de");

    expect(await Language.setAdditionalTranslationFile(json, TEST_CONTEXT.translate)).toEqual({ lang: "de", translations: Object({ key: "value" }), shouldMerge: true });
  });

  it("#setAdditionalTranslationFile - translation de not found - warning expected", async () => {
    const json = {
      "de": {
        "key": "value",
      },
    };

    spyOn(console, "warn");
    TEST_CONTEXT.translate.use("cz");
    await Language.setAdditionalTranslationFile(json, TEST_CONTEXT.translate);
    expect(console.warn).toHaveBeenCalledWith("No translation available for Language cz. Implemented languages are: de");
  });
});
