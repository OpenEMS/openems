import { DecimalPipe, registerLocaleData } from "@angular/common";
import localDE from '@angular/common/locales/de';
import { UnitvaluePipe } from "./unitvalue.pipe";
import { Language } from "../../type/language";

describe('UnitvaluePipe', () => {
    registerLocaleData(localDE);

    const pipe = new UnitvaluePipe(new DecimalPipe(Language.EN.key));
    // TODO test for more i18n-locales
    // Note: "locale" value in DecimalPipe sets itself to default locale ('en-US') even though we specify our own locales.

    it('transforms "1000 W" to "1.000 W"', () => {
        expect(pipe.transform(1000, 'W')).toBe('1.000' + '\u00A0' + 'W');
    });

    it('transforms "null W" to "- "', () => {
        expect(pipe.transform(null, 'W')).toBe('-' + '\u00A0');
    });
    it('transforms "undefined W" to "- "', () => {
        expect(pipe.transform(undefined, 'W')).toBe('-' + '\u00A0');
    });

    it('transforms "abc W" to "- "', () => {
        expect(pipe.transform("abc", 'W')).toBe('-' + '\u00A0');
    });

    it('transforms non number to "-"', () => {
        expect(pipe.transform(pipe, 'W')).toBe('-' + '\u00A0');
    });

    it('transforms "100 a" to "100 a"', () => {
        expect(pipe.transform(100, 'a')).toBe('100' + '\u00A0' + 'a');
    });

});
