import { DecimalPipe } from "@angular/common";
import { UnitvaluePipe } from "./unitvalue.pipe";

describe('UnitvaluePipe', () => {
    const pipe = new UnitvaluePipe(new DecimalPipe('en-US'));
    // TODO test for more i18n-locales

    it('transforms "1000 W" to "1,000 W"', () => {
        expect(pipe.transform(1000, 'W')).toBe('1,000' + '\u00A0' + 'W');
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

    it('transforms "1000 W" to "1,000 W"', () => {
        expect(pipe.transform(pipe, 'W')).toBe('-' + '\u00A0');
    });

    it('transforms "1000 W" to "1,000 W"', () => {
        expect(pipe.transform(100, 'a')).toBe('100' + '\u00A0' + 'a');
    });

});
