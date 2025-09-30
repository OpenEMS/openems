import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Edge, Producttype, Service, Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { TimeUtils } from "src/app/shared/utils/time/timeutils";
import { environment } from "src/environments";

@Component({
    selector: "oe-offline",
    templateUrl: "./OFFLINE.COMPONENT.HTML",
    styles: [`
            ion-item > ion-label > h3 {
                font-weight: bolder;
            }
        `],
    standalone: false,
})
export class OfflineComponent implements OnInit {

    protected edge: Edge | null = null;
    protected timeSinceOffline: string | null = null;
    protected isAtLeastInstaller: boolean = false;
    protected readonly environment = environment;
    protected readonly Producttype = Producttype;

    constructor(
        public service: Service,
        private translate: TranslateService,
    ) { }

    /**
     * Formats a valid
     *
     * @param ms the milli seconds
     * @param translate the translate service
     * @returns a string if passed milli seconds are not null, else null
     */
    public static formatMilliSecondsToValidRange(ms: number, translate: TranslateService): string {
        const TWO_DAYS = 2 * 24 * 60 * 60 * 1000;
        const TWO_HOURS = 2 * 60 * 60 * 1000;
        let translationKey: { singular: string, plural: string } = { singular: "GENERAL.TIME.MINUTE", plural: "GENERAL.TIME.MINUTES" };
        let convertedSeconds: number = TIME_UTILS.GET_MINUTES_FROM_MILLI_SECONDS(ms) ?? 0;

        if (ms > TWO_DAYS) {
            convertedSeconds = TIME_UTILS.GET_DAYS_FROM_MILLI_SECONDS(ms) ?? 0;
            translationKey = { singular: "GENERAL.TIME.DAY", plural: "GENERAL.TIME.DAYS" };
        } else if (ms > TWO_HOURS) {
            convertedSeconds = TIME_UTILS.GET_HOURS_FROM_MILLI_SECONDS(ms) ?? 0;
            translationKey = { singular: "GENERAL.TIME.HOUR", plural: "GENERAL.TIME.HOURS" };
        }

        return TIME_UTILS.GET_DURATION_TEXT(convertedSeconds, translate, TRANSLATION_KEY.SINGULAR, TRANSLATION_KEY.PLURAL);
    }

    /**
     * Gets a formatted text representing the time since the edge has been offline
     *
     * @param date the date string
     * @param translate the translate service
     * @returns a string if date is convertable to a Date, else null
     */
    private static getTimeSinceEdgeIsOffline(date: string, translate: TranslateService): string | null {

        const _date: Date | null = DATE_UTILS.STRING_TO_DATE(date);
        if (!_date) {
            return null;
        }

        const milliSeconds: number = _date.getTime();
        const _diff: number | null = UTILS.SUBTRACT_SAFELY(new Date().getTime(), milliSeconds);

        if (_diff === null) {
            return null;
        }
        return OFFLINE_COMPONENT.FORMAT_MILLI_SECONDS_TO_VALID_RANGE(_diff, translate);
    }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
            THIS.IS_AT_LEAST_INSTALLER = THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER);
            THIS.TIME_SINCE_OFFLINE = OFFLINE_COMPONENT.GET_TIME_SINCE_EDGE_IS_OFFLINE(EDGE.LASTMESSAGE?.toString(), THIS.TRANSLATE);
        });
    }
}
