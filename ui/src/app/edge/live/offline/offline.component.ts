import { Component, OnInit } from "@angular/core";
import { Edge, Service, Utils } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

// TODO add translations when refactoring offline.component.html
@Component({
    selector: "offline",
    templateUrl: "./offline.component.html",
})
export class OfflineComponent implements OnInit {

    protected edge: Edge | null = null;
    protected timeSinceOffline: string | null = null;

    constructor(
        public service: Service,
    ) { }

    private static formatSecondsToFullMinutes(date: string): null | string {

        const _date: Date | null = DateUtils.stringToDate(date);
        if (!_date) {
            return null;
        }

        const milliSeconds: number = _date.getTime();
        const _diff: number | null = Utils.subtractSafely(new Date().getTime(), milliSeconds);

        if (_diff === null) {
            return null;
        }

        if (_diff > 2 * 24 * 60 * 60 * 1000) {
            return Utils.floorSafely(Utils.divideSafely(_diff, 24 * 60 * 60 * 1000)) + " Tagen";
        }

        if (_diff > 2 * 60 * 60 * 1000) {
            return Utils.floorSafely(Utils.divideSafely(_diff, 60 * 60 * 1000)) + " Stunden";
        }

        const minutes: number | null = Utils.floorSafely(Utils.divideSafely(_diff, 60 * 1000));
        return Utils.floorSafely(Utils.divideSafely(_diff, 60 * 1000)) + " " + (minutes === 1 ? "Minute" : "Minuten");
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.timeSinceOffline = OfflineComponent.formatSecondsToFullMinutes(edge.lastmessage.toString());
        });
    }
}
