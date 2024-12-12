import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Edge, Producttype, Service, Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { TimeUtils } from "src/app/shared/utils/time/timeutils";
import { environment } from "src/environments";

@Component({
    selector: "oe-offline",
    templateUrl: "./offline.component.html",
    styles: [`
            ion-item > ion-label > h3 {
                font-weight: bolder;
            }
        `],
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
        let translationKey: { singular: string, plural: string } = { singular: "General.TIME.MINUTE", plural: "General.TIME.MINUTES" };
        let convertedSeconds: number = TimeUtils.getMinutesFromMilliSeconds(ms) ?? 0;

        if (ms > TWO_DAYS) {
            convertedSeconds = TimeUtils.getDaysFromMilliSeconds(ms) ?? 0;
            translationKey = { singular: "General.TIME.DAY", plural: "General.TIME.DAYS" };
        } else if (ms > TWO_HOURS) {
            convertedSeconds = TimeUtils.getHoursFromMilliSeconds(ms) ?? 0;
            translationKey = { singular: "General.TIME.HOUR", plural: "General.TIME.HOURS" };
        }

        return TimeUtils.getDurationText(convertedSeconds, translate, translationKey.singular, translationKey.plural);
    }

    /**
     * Gets a formatted text representing the time since the edge has been offline
     *
     * @param date the date string
     * @param translate the translate service
     * @returns a string if date is convertable to a Date, else null
     */
    private static getTimeSinceEdgeIsOffline(date: string, translate: TranslateService): string | null {

        const _date: Date | null = DateUtils.stringToDate(date);
        if (!_date) {
            return null;
        }

        const milliSeconds: number = _date.getTime();
        const _diff: number | null = Utils.subtractSafely(new Date().getTime(), milliSeconds);

        if (_diff === null) {
            return null;
        }
        return OfflineComponent.formatMilliSecondsToValidRange(_diff, translate);
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.isAtLeastInstaller = this.edge.roleIsAtLeast(Role.INSTALLER);
            this.timeSinceOffline = OfflineComponent.getTimeSinceEdgeIsOffline(edge.lastmessage?.toString(), this.translate);
        });
    }
}
