import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { EvcsChargeModeViewModel } from "../../shared/shared";

@Component({
    selector: "oe-evse-charge-mode",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
})
export class ChargeModeComponent extends AbstractFormlyComponent<EvcsChargeModeViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;
    protected modeChannel: any;

    constructor(protected override service: Service) {
        super();
    }

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): OeFormlyView<EvcsChargeModeViewModel> {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const hasKebaComponent: boolean =
            (edge.getCurrentConfig()?.getComponentsByFactory("Evse.ChargePoint.Keba.Modbus")?.length ?? 0) > 0;

        const lines: OeFormlyField<EvcsChargeModeViewModel>[] = [
            {
                type: "info-line",
                name: translate.instant("EVSE_SINGLE.SETTINGS.CHARGE_MODE"),
                style: {
                    name: {
                        fontWeight: "bold",
                        textAlign: "center",
                        fontSize: "1rem",
                        paddingBottom: "calc(var(--ion-padding) * 4)",
                    },
                },
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
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.SURPLUS"),
                        value: Mode.SURPLUS,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.MINIMUM"),
                        value: Mode.MINIMUM,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_CHARGE"),
                        value: Mode.FORCE,
                    },
                ],
            },
            {
                type: "info-line",
                name: [
                    {
                        text: translate.instant("EDGE.INDEX.WIDGETS.EVSE.KEBA_WARNING"),
                        lineStyle: "color:#d32f2f; padding:6px 12px; border:1px solid #d32f2f; border-radius:4px;",
                    },
                ],
                link: {
                    text: translate.instant("EDGE.INDEX.WIDGETS.EVCS.LINK_TO_DOCUMENTATION"),
                    href: "https://docs.intranet.fenecon.de/feature/how_to_restart_KEBA_P40/fenecon/de/emobility/Installationsanleitung_KEBA_P40.html#_kommunikationsausfall_zwischen_keba_p40p40_pro_und_fems",
                },
                hide: (el) =>
                    (el.mode !== Mode.SURPLUS && el.mode !== Mode.ZERO) || component == null || !hasKebaComponent,
            },
        ];
        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithChannel<number>(this.form(), "mode", currentData, this.modeChannel);
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView<EvcsChargeModeViewModel> {
        this.component = this.getComponent();
        return ChargeModeComponent.generateView(viewContext.translate, this.component, viewContext.edge);
    }

    protected override getFormGroup(): FormGroup {
        this.component ??= this.getComponent();
        AssertionUtils.assertIsDefined(this.component);
        return new FormGroup({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();

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
