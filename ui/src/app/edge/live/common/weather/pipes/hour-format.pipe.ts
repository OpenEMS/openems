import { Pipe, PipeTransform } from "@angular/core";
import { Language } from "src/app/shared/type/language";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Pipe({ name: "hourFormat" })
export class HourFormatPipe implements PipeTransform {
    transform(date: Date): string {
        return DateTimeUtils.formatHour(date, Language.getCurrentLanguage());
    }
}
