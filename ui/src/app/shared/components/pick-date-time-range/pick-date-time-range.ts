import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from "@angular/core";
import { FormControl } from "@angular/forms";
import { IonPopover } from "@ionic/angular";

@Component({
    selector: "oe-pick-date-time-range",
    templateUrl: "./pick-date-time-range.html",
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
            this.updateDisplayText();
            this.updateBorderColor();
        }
    }

    public ngAfterViewInit() {
        // Border color update just in case the display value was present at init
        this.updateBorderColor();
    }

    public isValidTime(time: string | null | undefined): boolean {
        if (time === null || time === undefined) {
            return false;
        }

        return /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/.test(time);
    }

    protected presentPopover() {
        if (this.triggerItem?.nativeElement) {
            this.popoverEvent = {
                target: this.triggerItem.nativeElement,
            };
            this.popoverInstance.present();
        }
    }

    protected handleTimeChange(event: CustomEvent) {
        const value = event.detail.value;
        this.timeChange.emit(this.formatValue(value));
        this.popoverInstance.dismiss();
        this.updateBorderColor();
    }

    private updateDisplayText(): void {
        if (this.displayValue === null || this.displayValue === "") {
            this.displayText = this.mode === "date"
                ? "TT.MM.JJJJ"
                : "00:00";
        } else {
            this.displayText = this.displayValue;
        }
    }

    private updateBorderColor() {
        // broder color generation is valid for "time" mode. seperate valid functions required for other modes.
        let newBorderColor = "var(--ion-color-medium-tint)";

        if (this.mode === "time") {
            const isValidTime = this.isValidTime(this.displayValue);

            if (isValidTime) {
                newBorderColor = this.invalid
                    ? "var(--highlight-color-invalid, var(--ion-color-danger))"
                    : "var(--highlight-color-valid, var(--ion-color-success))";
            }
        }

        this.borderColor = newBorderColor;
    }


    private formatValue(value: string | Date | null): string {
        if (value === null || value === "") {
            return this.mode === "date" ? "TT.MM.JJJJ" : "00:00";
        }

        const date = new Date(value);

        if (this.mode === "date") {
            return date.toLocaleDateString("default");
        } else if (this.mode === "datetime") {
            return `${date.toLocaleTimeString("deafult", { hour: "2-digit", minute: "2-digit" })} ${date.toLocaleDateString("deafult")}`;
        } else {
            return date.toLocaleTimeString("default", { hour: "2-digit", minute: "2-digit" });
        }
    }
}
