import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
    ],
})
export class ModalComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    // Increased skip count
    protected override SKIP_COUNT: number = 8;
    private component: EdgeConfig.Component | null = null;
    private energySessionLimitChannel: ChannelAddress | null = null;

    constructor(
        private route: ActivatedRoute,
        private service: Service,
    ) {
        super();
    }

    public static generateView(translate: TranslateService, component: EdgeConfig.Component | null, edge: Edge | null): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const lines: OeFormlyField[] = [
            {
                type: "value-from-form-control-line",
                name: "Energielimit",
                controlName: "manualEnergySessionLimit",
                converter: Converter.WATT_HOURS_IN_KILO_WATT_HOURS,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "manualEnergySessionLimit",
                properties: { tickMin: 0, tickMax: 100000, step: 1000, tickFormatter: (val) => Converter.WATT_HOURS_IN_KILO_WATT_HOURS(val), pinFormatter: (val) => Converter.WATT_HOURS_IN_KILO_WATT_HOURS(val) },
            }];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {

        if (this.skipCurrentData || this.form.dirty || this.form.touched || !this.energySessionLimitChannel || currentData.allComponents[this.energySessionLimitChannel.toString()] == null) {
            return;
        }

        const manualEnergySessionLimit = currentData.allComponents[this.energySessionLimitChannel.toString()];
        this.form.controls["manualEnergySessionLimit"].setValue(manualEnergySessionLimit);
        this.form.controls["manualEnergySessionLimit"].markAsTouched();
    }

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        this.component = config.getComponent(this.route.snapshot.params.componentId);
        const edge = this.service.currentEdge();
        return ModalComponent.generateView(this.translate, this.component, edge);
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            manualEnergySessionLimit: new FormControl(null),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {

        const config = await this.service.getConfig();
        const component = config.getComponent(this.route.snapshot.params.componentId);
        if (!component || !component.id) {
            return [];
        }
        this.energySessionLimitChannel = new ChannelAddress(component.id, "_PropertyManualEnergySessionLimit");
        return [this.energySessionLimitChannel];
    }
}
