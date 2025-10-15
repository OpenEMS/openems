import { Pipe, PipeTransform } from "@angular/core";
import { Language } from "src/app/shared/type/language";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

@Pipe({ name: "dayMonthFormat" })
export class DayMonthFormatPipe implements PipeTransform {
    transform(date: Date): string {
        return DateUtils.formatDayMonth(date, Language.getCurrentLanguage());
    }
}
