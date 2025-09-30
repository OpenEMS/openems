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
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/TEMPLATE.HTML",
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

export class EvseSettingsComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    // Increased skip count
    private component: EDGE_CONFIG.COMPONENT | null = null;
    private energySessionLimitChannel: ChannelAddress | null = null;

    constructor(
        private route: ActivatedRoute,
        private service: Service,
    ) {
        super();
    }

    public static generateView(translate: TranslateService, component: EDGE_CONFIG.COMPONENT | null, edge: Edge | null): OeFormlyView {
        ASSERTION_UTILS.ASSERT_IS_DEFINED(component);
        ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);

        const lines: OeFormlyField[] = [
            {
                type: "value-from-form-control-line",
                name: TRANSLATE.INSTANT("EVSE_SINGLE.SETTINGS.ENERGY_LIMIT"),
                controlName: "manualEnergySessionLimit",
                converter: Converter.WATT_HOURS_IN_KILO_WATT_HOURS,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "manualEnergySessionLimit",
                properties: {
                    tickMin: 0,
                    tickMax: 100000,
                    step: 1000,
                    tickFormatter: (val) => Converter.WATT_HOURS_IN_KILO_WATT_HOURS(val),
                    pinFormatter: (val) => Converter.WATT_HOURS_IN_KILO_WATT_HOURS(val),
                },
            }];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        THIS.SET_FORM_CONTROL_SAFELY<number>(THIS.FORM, "manualEnergySessionLimit", currentData, THIS.ENERGY_SESSION_LIMIT_CHANNEL);
    }

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
        const edge = THIS.SERVICE.CURRENT_EDGE();
        return EVSE_SETTINGS_COMPONENT.GENERATE_VIEW(THIS.TRANSLATE, THIS.COMPONENT, edge);
    }

    protected override getFormGroup(): FormGroup {
        if (OBJECT.KEYS(THIS.FORM.CONTROLS).length > 0) {
            return THIS.FORM;
        }
        return new FormGroup({
            manualEnergySessionLimit: new FormControl(null),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {

        const config = await THIS.SERVICE.GET_CONFIG();
        const component = CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
        if (!component || !COMPONENT.ID) {
            return [];
        }
        THIS.ENERGY_SESSION_LIMIT_CHANNEL = new ChannelAddress(COMPONENT.ID, "_PropertyManualEnergySessionLimit");
        return [THIS.ENERGY_SESSION_LIMIT_CHANNEL];
    }
}
