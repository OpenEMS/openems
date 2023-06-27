import { DecimalPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

import { Language } from '../../type/language';

@Pipe({
    name: 'unitvalue'
})
export class UnitvaluePipe implements PipeTransform {

    constructor(private decimalPipe: DecimalPipe) { }

    transform(value: any, unit: string): any {
        if (value == null || value == undefined
            || (typeof value === 'string' && value.trim() === "")
            || typeof value === 'boolean' || isNaN(value)) {
            return '-' + '\u00A0';
        } else {
            // Changes the number format based on the language selected.
            const locale: string = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).i18nLocaleKey;

            if (unit == 'kWh' || unit == 'kW') {
                return this.decimalPipe.transform(value / 1000, '1.0-1', locale) + '\u00A0' + unit;
            } else {
                return this.decimalPipe.transform(value, '1.0-0', locale) + '\u00A0' + unit;
            }
        }
    }
}