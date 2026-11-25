import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { HeatingElementComponent, HeatingElementMode } from "../util/utils";

@Component({
    selector: "oe-controller-io-heating-element-base-mode",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerIoHeatingElementBaseModeComponent extends AbstractFormlyComponent<{
    mode: HeatingElementMode | null;
}> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<{ mode: HeatingElementMode | null }> {
        return {
            title: translate.instant("GENERAL.BASE_MODE"),
            icon: { name: "oe-heating-element", color: "var(--ion-color-svg)", size: "small" },
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            component,
            edge,
            lines: [
                {
                    type: "image-line",
                    img: {
                        url: environment.icons.COMPONENT.HEATING_ELEMENT,
                        width: 50,
                        color: "var(--ion-color-svg)",
                        style: {
                            maxWidth: "16rem",
                            margin: "0 auto",
                            paddingBottom: "var(--ion-padding)",
                        },
                    },
                },
                {
                    type: "radio-buttons-from-form-control-line",
                    name: "select-mode",
                    controlName: "mode",
                    buttons: [
                        {
                            name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.FAST_HEAT"),
                            description: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.FAST_HEAT_DESCRIPTION"),
                            value: HeatingElementMode.FAST_HEAT,
                            icon: { color: "success", name: "oe-consumption", size: "medium" },
                        },
                        {
                            name: translate.instant("HEAT.SETTINGS.MODE.SURPLUS.TITLE"),
                            description: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.PV_SURPLUS_DESCRIPTION"),
                            value: HeatingElementMode.AUTOMATIC,
                            icon: { color: "primary", name: "oe-production", size: "medium" },
                        },
                        {
                            name: translate.instant("HEAT.SETTINGS.MODE.OFF.TITLE"),
                            description: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.NO_HEATING_DESCRIPTION"),
                            value: HeatingElementMode.MANUAL_OFF,
                            icon: { color: "danger", name: "power-outline", size: "medium" },
                        },
                    ],
                },
            ],
        };
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView<{ mode: HeatingElementMode | null }> {
        return ControllerIoHeatingElementBaseModeComponent.generateView(
            viewContext.translate,
            this.getComponent(),
            viewContext.edge,
        );
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({ mode: new FormControl<HeatingElementMode | null>(null) });
    }

    protected override buildUpdateComponentArr(fg: FormGroup): { name: string; value: unknown }[] {
        return new HeatingElementComponent(this.getComponent(), this.getEdge()).filterSupportedUpdates(
            super.buildUpdateComponentArr(fg),
        );
    }

    protected override applyChanges(
        fg: FormGroup,
        service: Service,
        websocket: Websocket,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): void {
        if (this.buildUpdateComponentArr(fg).length === 0) {
            fg.markAsPristine();
            return;
        }
        super.applyChanges(fg, service, websocket, component, edge);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return [new ChannelAddress(this.getComponent().id, "_PropertyMode")];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithChannel(
            this.form(),
            "mode",
            currentData,
            new ChannelAddress(this.getComponent().id, "_PropertyMode"),
        );
    }
}
