import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { RouteService } from "../../../../../shared/service/route.service";
import { PropertyMode, SharedControllerHeat } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerHeatSettingsComponent extends AbstractFormlyComponent {
    public static readonly FORM_CONTROL_NAME = "mode";
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;

    private readonly routeService: RouteService = inject(RouteService);

    public static generateView(
        component: EdgeConfig.Component | null,
        edge: Edge | null,
        translate: TranslateService,
    ): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const isAskoma = component.factoryId === "Heat.Askoma";
        const isMyPv = component.factoryId === "Heat.MyPv";
        const isWritable = component.properties?.readOnly !== true;
        return {
            title: Name.METER_ALIAS_OR_ID(component),
            icon: { name: "oe-heating-element", color: "normal", size: "normal" },
            lines: [
                ...(isAskoma ? SharedControllerHeat.getAskomaIcon() : []),
                ...(isMyPv ? SharedControllerHeat.getMyPvIcon() : []),
                ...(isWritable ? SharedControllerHeat.getFormlySettingsLines(translate) : []),
            ],
            component,
            edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component === null) {
            return;
        }

        this.setFormControlSafelyWithChannel<PropertyMode>(
            this.form,
            ControllerHeatSettingsComponent.FORM_CONTROL_NAME,
            currentData,
            new ChannelAddress(this.component.id, "_PropertyMode"),
        );
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        this.component = viewContext.config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        const edge = this.service.currentEdge();
        return ControllerHeatSettingsComponent.generateView(this.component, edge, viewContext.translate);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerHeat.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));

        if (this.component?.id == null || this.isReadOnly()) {
            return [];
        }

        return [new ChannelAddress(this.component.id, "_PropertyMode")];
    }

    protected isReadOnly(): boolean {
        return this.component == null || this.component?.properties?.readOnly === true;
    }
}

export enum Mode {
    OFF = "OFF",
    FAST_HEAT = "FAST_HEAT",
    SURPLUS = "SURPLUS",
}
