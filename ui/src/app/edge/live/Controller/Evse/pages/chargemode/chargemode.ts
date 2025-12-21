import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
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

export class ChargeModeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;
    protected modeChannel: any;

    constructor(
        protected override service: Service,
        private route: ActivatedRoute,
    ) {
        super();
    }

    public static generateView(translate: TranslateService, component: EdgeConfig.Component | null, edge: Edge | null): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const lines: OeFormlyField[] = [
            {
                type: "info-line",
                name: translate.instant("EVSE_SINGLE.SETTINGS.CHARGE_MODE"),
                style: "font-weight: bold; text-align: center; font-size: 1rem; padding-bottom: calc(var(--ion-padding) * 4)",
            },
            {
                type: "radio-buttons-from-form-control-line",
                name: "phase-switching",
                controlName: "mode", // propertyname
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.ZERO"),
                        value: Mode.ZERO,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.MINIMUM"),
                        value: Mode.MINIMUM,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.SURPLUS"),
                        value: Mode.SURPLUS,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_CHARGE"),
                        value: Mode.FORCE,
                    },
                ],
            }];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithChannel<number>(this.form, "mode", currentData, this.modeChannel);
    }

    protected override generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {
        this.component = config.getComponent(this.route.snapshot.params.componentId);
        const edge = this.service.currentEdge();
        return ChargeModeComponent.generateView(translate, this.component, edge);
    }

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component);
        return new FormGroup({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {

        const config = await this.service.getConfig();
        const component = config.getComponent(this.route.snapshot.params.componentId);

        if (component === undefined || component.id === undefined) {
            return [];
        }
        this.modeChannel = new ChannelAddress(component.id, "_PropertyMode");
        return [this.modeChannel];
    }
}

export enum Mode {
    ZERO = "ZERO", //
    MINIMUM = "MINIMUM", //
    SURPLUS = "SURPLUS", //
    FORCE = "FORCE", //
}
