import { Pipe, PipeTransform } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";

@Pipe({
    name: "weekDay",
    standalone: true,
})
export class WeekDayFormatPipe implements PipeTransform {

    constructor(private translate: TranslateService) { }

    transform(value: any): any {
        switch (value) {
            case "mo":
                return this.translate.instant("GENERAL.WEEK.MONDAY");
            case "tu":
                return this.translate.instant("GENERAL.WEEK.TUESDAY");
            case "we":
                return this.translate.instant("GENERAL.WEEK.WEDNESDAY");
            case "th":
                return this.translate.instant("GENERAL.WEEK.THURSDAY");
            case "fr":
                return this.translate.instant("GENERAL.WEEK.FRIDAY");
            case "sa":
                return this.translate.instant("GENERAL.WEEK.SATURDAY");
            case "su":
                return this.translate.instant("GENERAL.WEEK.SUNDAY");
        }
    }
}
