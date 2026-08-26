import { Pipe, PipeTransform } from "@angular/core";
import { Language } from "src/app/shared/type/language";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

@Pipe({ name: "weekdayFormat" })
export class WeekdayFormatPipe implements PipeTransform {
    transform(date: Date): string {
        return DateUtils.formatWeekday(date, Language.getCurrentLanguage());
    }
}
