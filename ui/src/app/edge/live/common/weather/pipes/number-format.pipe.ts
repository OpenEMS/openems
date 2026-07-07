import { formatNumber } from "@angular/common";
import { Pipe, PipeTransform } from "@angular/core";
import { Language } from "src/app/shared/type/language";

@Pipe({ name: "numberFormat" })
export class NumberFormatPipe implements PipeTransform {
    transform(value: number, digitsInfo: string): string {
        const language = Language.getCurrentLanguage();
        return formatNumber(value, language.i18nLocaleKey, digitsInfo);
    }
}
