import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TZDate } from "@date-fns/tz";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { addDays } from "date-fns";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { GetOneTasks } from "../shared/getOneTasks";
import { SharedControllerIoHeatingRoom } from "../shared/shared";

@Component({
    selector: "oe-controller-io-heating-room-home",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerIoHeatingRoomHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;

    private static getLines(
        translate: TranslateService,
        component: EdgeConfig.Component,
        oneTasks: GetOneTasks.OneTask[],
    ): OeFormlyField[] {
        const hideWhenOff = (value: { mode?: string }) => value?.mode === SharedControllerIoHeatingRoom.Mode.OFF;
        const hideWhenNotAutomatic = (value: { mode?: string }) =>
            value?.mode !== SharedControllerIoHeatingRoom.Mode.AUTOMATIC;

        const lines: OeFormlyField[] = [
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MODE"),
                channel: component.id + "/" + SharedControllerIoHeatingRoom.PROPERTY_MODE,
                converter: SharedControllerIoHeatingRoom.convertToModeLabel(translate),
            },
            {
                type: "channel-line",
                name: translate.instant("IO_HEATING_ROOM.HOME.POWER_CONSUMPTION"),
                channel: component.id + "/ActivePower",
                converter: Converter.POWER_IN_WATT,
                hide: hideWhenOff,
            },
            { type: "horizontal-line" },
            {
                type: "channel-line",
                name: translate.instant("IO_HEATING_ROOM.HOME.FLOOR_ACTUAL"),
                channel: component.id + "/FloorActual",
                converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
            },
            {
                type: "channel-line",
                name: translate.instant("IO_HEATING_ROOM.HOME.FLOOR_TARGET"),
                channel: component.id + "/FloorTarget",
                converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
                hide: hideWhenOff,
            },
            { type: "horizontal-line" },
            {
                type: "channel-line",
                name: translate.instant("IO_HEATING_ROOM.HOME.AMBIENT_ACTUAL"),
                channel: component.id + "/AmbientActual",
                converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
            },
            {
                type: "channel-line",
                name: translate.instant("IO_HEATING_ROOM.HOME.AMBIENT_TARGET"),
                channel: component.id + "/AmbientTarget",
                converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
                hide: hideWhenOff,
            },
        ];

        if (oneTasks.length > 0) {
            lines.push(
                { type: "horizontal-line", hide: hideWhenNotAutomatic },
                {
                    type: "info-line",
                    name: translate.instant("IO_HEATING_ROOM.HOME.UPCOMING_EVENTS"),
                    html: oneTasks.map((task) => `<p>${task.start} - ${task.end} (${task.duration})</p>`).join(""),
                    hide: hideWhenNotAutomatic,
                },
            );
        }

        return lines;
    }

    protected override async generateView(viewContext: ViewContext): Promise<OeFormlyView> {
        this.component = viewContext.config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);

        const oneTasks = await this.getOneTasks(viewContext.edge, this.component);

        return {
            title: this.component.alias,
            icon: { name: "flame", color: "danger", size: "large" },
            lines: ControllerIoHeatingRoomHomeComponent.getLines(viewContext.translate, this.component, oneTasks),
            component: this.component,
            edge: viewContext.edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        this.setFormControlSafelyWithChannel<string>(
            this.form(),
            "mode",
            currentData,
            new ChannelAddress(this.component.id, SharedControllerIoHeatingRoom.PROPERTY_MODE),
        );
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({ mode: new FormControl(null) });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));

        if (this.component?.id == null) {
            return [];
        }

        return [
            ...SharedControllerIoHeatingRoom.getChannelAddresses(this.component),
            new ChannelAddress(this.component.id, "ActivePower"),
            new ChannelAddress(this.component.id, "FloorActual"),
            new ChannelAddress(this.component.id, "FloorTarget"),
            new ChannelAddress(this.component.id, "AmbientActual"),
            new ChannelAddress(this.component.id, "AmbientTarget"),
        ];
    }

    private async getOneTasks(edge: Edge, component: EdgeConfig.Component): Promise<GetOneTasks.OneTask[]> {
        if (component.properties.mode !== SharedControllerIoHeatingRoom.Mode.AUTOMATIC) {
            return [];
        }

        const from = new TZDate();
        const to = addDays(from, 2);

        try {
            const response = await edge.sendRequest<GetOneTasks.Response>(
                this.service.websocket,
                new ComponentJsonApiRequest({
                    componentId: component.id,
                    payload: new GetOneTasks.Request({
                        from: from.toISOString(),
                        to: to.toISOString(),
                    }),
                }),
            );
            return response.result.oneTasks;
        } catch (reason) {
            console.warn(reason);
            return [];
        }
    }
}
