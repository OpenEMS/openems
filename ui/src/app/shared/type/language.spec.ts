// @ts-strict-ignore
import { Language } from "./language";

describe('Language', () => {

  it('#geti18nLocaleByKey', () => {
    expect(Language.geti18nLocaleByKey('DE')).toBe('de');
    expect(Language.geti18nLocaleByKey('Zz')).toBe(Language.DEFAULT.i18nLocaleKey);
    expect(Language.geti18nLocaleByKey(null)).toBe(Language.DEFAULT.i18nLocaleKey);
    expect(Language.geti18nLocaleByKey(undefined)).toBe(Language.DEFAULT.i18nLocaleKey);
  });
});
