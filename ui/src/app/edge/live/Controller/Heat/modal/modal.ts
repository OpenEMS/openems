import { Component, Input, ChangeDetectionStrategy } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { PropertyMode, SharedControllerHeat } from "../shared/shared";

@Component({
    selector: "heat-modal",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerHeatModalComponent extends AbstractFormlyComponent {
    public static readonly formControlName = "mode";

    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public edge: Edge | null = null;

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): OeFormlyView {
        return SharedControllerHeat.getFormlyModalView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView {
        return ControllerHeatModalComponent.generateView(this.translate, this.component, this.edge);
    }

    protected override getFormGroup(): FormGroup {
        if (this.isReadOnly()) {
            return new FormGroup({});
        }

        return SharedControllerHeat.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        if (this.component == null) {
            return [];
        }

        return SharedControllerHeat.getChannelAddressesForComponent(this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const readOnly = this.isReadOnly();
        const channelAddress =
            !readOnly && this.component != null ? new ChannelAddress(this.component.id, "_PropertyMode") : null;
        this.setFormControlSafelyWithChannel<PropertyMode>(
            this.form,
            ControllerHeatModalComponent.formControlName,
            currentData,
            channelAddress,
        );
    }

    private isReadOnly(): boolean {
        return this.component == null || this.component?.properties?.readOnly === true;
    }
}
