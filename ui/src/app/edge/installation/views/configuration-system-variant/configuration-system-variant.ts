// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { AbstractIbn } from "../../installation-systems/abstract-ibn";
import { System, SystemId } from "../../shared/system";

@Component({
    selector: ConfigurationSystemVariantComponent.SELECTOR,
    templateUrl: './configuration-system-variant.html',
})
export class ConfigurationSystemVariantComponent implements OnInit {

    private static readonly SELECTOR = 'configuration-system-variant';

    @Input() public ibn: AbstractIbn;
    @Output() public nextViewEvent = new EventEmitter();
    @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

    public form: FormGroup;
    public fields: FormlyFieldConfig[];
    public model;

    public ngOnInit() {
        this.form = new FormGroup({});
        this.fields = this.ibn.getSystemVariantFields();
    }

    public onNextClicked() {
        if (this.form.invalid) {
            return;
        }
        // Sets the ibn object.
        const system = this.form.controls.system.value;
        this.ibn = this.ibn.setIbn(system);

        this.ibn.showViewCount = true;
        this.nextViewEvent.emit(this.ibn);
    }

    public onPreviousClicked() {
        this.previousViewEvent.emit();
    }

    /**
     * Redirects to the appropriate url for system manual.
     */
    protected openManual(): void {
        const homeSystemVariant = this.form.controls.system?.value as SystemId.FENECON_HOME_10 | SystemId.FENECON_HOME_20 | SystemId.FENECON_HOME_30;
        window.open(System.getHomeSystemInstructionsLink(homeSystemVariant));
    }

    /**
     * Displays the button for home systems, otherwise false.
     *
     * @returns {boolean} True if the selected system is a home system, false otherwise.
     *
     * @remarks
     * This getter is used to determine whether the system selected in the form is one of the predefined home systems.
     * It checks if the selected system value exists and is included in the `homeSystemIds` array.
     *
     */
    protected get showManualButton(): boolean {
        const selectedSystem: SystemId = this.form.controls.system?.value;
        return System.isHomeSystemSelected(selectedSystem);
    }
}
