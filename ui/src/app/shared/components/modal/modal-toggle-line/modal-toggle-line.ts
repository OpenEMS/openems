import { Component, Input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractModalLine } from "../abstract-modal-line";

@Component({
    selector: "oe-modal-toggle-line",
    templateUrl: "./modal-toggle-line.html",
    imports: [
        CommonUiModule,
        ReactiveFormsModule,
    ],
})
export class ModalToggleLineComponent extends AbstractModalLine {

    // Width of Left Column, Right Column is (100% - leftColumn)
    @Input({ required: true }) protected leftColumnWidth!: number;

    @Input() protected textIndent: TextIndentation = TextIndentation.NONE;
    @Input() public togglePrefix: (value: number | string | null) => string | null = (value) => null;

    public override ngOnInit() {
        super.ngOnInit();

        this.formGroup.valueChanges.subscribe(value => {
            this.displayValue = this.togglePrefix(value);
        });
    }

    /** Toggle */
    protected toggleOnEnter(event: KeyboardEvent, controlName: string) {
        const control = this.formGroup.get(controlName);
        if (control) {
            control.setValue(!control.value);
            event.preventDefault();
        }
    }
}

export enum TextIndentation {
    NONE = "0%",
    SINGLE = "5%",
    DOUBLE = "10%",
}
