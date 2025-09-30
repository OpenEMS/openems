import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from "@angular/core";
import { FormControl } from "@angular/forms";
import { IonPopover } from "@ionic/angular";

@Component({
    selector: "oe-pick-date-time-range",
    templateUrl: "./pick-date-time-RANGE.HTML",
    standalone: false,
    styles: [`
        .time-display-item {
            border: 0.067em; /* 1px in em */
            border-radius: 1em; /* 15px in em */
            border-style: solid;
            text-align: center;
        }
        `],
})
export class PickDateTimeRangeComponent implements OnChanges, AfterViewInit {

    @Input() public control!: FormControl;
    @Input() public mode: "time" | "date" | "datetime" = "time";
    @Input() public displayValue: string | null = null;
    @Input() public borderColor: string = "var(--ion-color-medium-tint)";
    @Input() public minuteValues!: string;
    @Input() public invalid: boolean = false;
    @Output() public timeChange = new EventEmitter<string>();

    @ViewChild("popoverInstance") public popoverInstance!: IonPopover;
    @ViewChild("triggerItem", { read: ElementRef }) public triggerItem!: ElementRef;

    protected popoverEvent: { target: HTMLElement } | null = null;
    protected displayText: string | null = null;

    public ngOnChanges(changes: SimpleChanges) {
        if ("displayValue" in changes || "invalid" in changes || "mode" in changes) {
            THIS.UPDATE_DISPLAY_TEXT();
            THIS.UPDATE_BORDER_COLOR();
        }
    }

    public ngAfterViewInit() {
        // Border color update just in case the display value was present at init
        THIS.UPDATE_BORDER_COLOR();
    }

    public isValidTime(time: string | null | undefined): boolean {
        if (time === null || time === undefined) {
            return false;
        }

        return /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/.test(time);
    }

    protected presentPopover() {
        if (THIS.TRIGGER_ITEM?.nativeElement) {
            THIS.POPOVER_EVENT = {
                target: THIS.TRIGGER_ITEM.NATIVE_ELEMENT,
            };
            THIS.POPOVER_INSTANCE.PRESENT();
        }
    }

    protected handleTimeChange(event: CustomEvent) {
        const value = EVENT.DETAIL.VALUE;
        THIS.TIME_CHANGE.EMIT(THIS.FORMAT_VALUE(value));
        THIS.POPOVER_INSTANCE.DISMISS();
        THIS.UPDATE_BORDER_COLOR();
    }

    private updateDisplayText(): void {
        if (THIS.DISPLAY_VALUE === null || THIS.DISPLAY_VALUE === "") {
            THIS.DISPLAY_TEXT = THIS.MODE === "date"
                ? "TT.MM.JJJJ"
                : "00:00";
        } else {
            THIS.DISPLAY_TEXT = THIS.DISPLAY_VALUE;
        }
    }

    private updateBorderColor() {
        // broder color generation is valid for "time" mode. seperate valid functions required for other modes.
        let newBorderColor = "var(--ion-color-medium-tint)";

        if (THIS.MODE === "time") {
            const isValidTime = THIS.IS_VALID_TIME(THIS.DISPLAY_VALUE);

            if (isValidTime) {
                newBorderColor = THIS.INVALID
                    ? "var(--highlight-color-invalid, var(--ion-color-danger))"
                    : "var(--highlight-color-valid, var(--ion-color-success))";
            }
        }

        THIS.BORDER_COLOR = newBorderColor;
    }


    private formatValue(value: string | Date | null): string {
        if (value === null || value === "") {
            return THIS.MODE === "date" ? "TT.MM.JJJJ" : "00:00";
        }

        const date = new Date(value);

        if (THIS.MODE === "date") {
            return DATE.TO_LOCALE_DATE_STRING("default");
        } else if (THIS.MODE === "datetime") {
            return `${DATE.TO_LOCALE_TIME_STRING("deafult", { hour: "2-digit", minute: "2-digit" })} ${DATE.TO_LOCALE_DATE_STRING("deafult")}`;
        } else {
            return DATE.TO_LOCALE_TIME_STRING("default", { hour: "2-digit", minute: "2-digit" });
        }
    }
}
