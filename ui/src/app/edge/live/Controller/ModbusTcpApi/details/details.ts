import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { ProfileComponent } from "src/app/edge/settings/profile/profile.component";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Filter } from "src/app/shared/components/shared/filter";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, ChannelRegister, CurrentData, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedControllerModbusTcpApiReadWrite } from "../shared/shared";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerModbusTcpApiDetailsComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component | null,
        service: Service,
    ): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        const channel = new ChannelAddress(component.id, "OverrideStatus");
        const writeChannelIds: string[] = (component.properties.writeChannels ?? []).filter(
            (channelId: string) => channelId !== "Ess0SetActivePowerEquals",
        );
        const writeChannels = writeChannelIds.map(
            (channelId) => new SharedControllerModbusTcpApiReadWrite.ModbusTcpApiChannel(component.id, channelId),
        );
        return {
            title: component.alias,
            lines: [
                {
                    type: "value-from-channels-line",
                    channelsToSubscribe: [channel],
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.CURRENT_STATE"),
                    value: (currentData: CurrentData) =>
                        SharedControllerModbusTcpApiReadWrite.TO_OVERRIDE_STATUS_LABEL(translate)(
                            currentData.allComponents[channel.toString()],
                        ),
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "name-line",
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.ACTIVE_POWER_LIMITATIONS"),
                },
                {
                    type: "name-line",
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.REGISTER") + " (SetActivePowerEquals)",
                },
                {
                    type: "channel-line",
                    channel: new ChannelAddress(component.id, "Ess0SetActivePowerEquals").toString(),
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.LIMITATION"),
                    converter: Converter.POWER_IN_WATT,
                },
                {
                    type: "channel-line",
                    channel: ChannelAddress.fromString("_sum/EssActivePower").toString(),
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.ACTUAL_VALUE"),
                    converter: Converter.POWER_IN_WATT,
                },
                {
                    type: "name-line",
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS"),
                },
                {
                    type: "horizontal-line",
                },
                ...ControllerModbusTcpApiDetailsComponent.getWriteChannelLines(writeChannels, translate),
                {
                    type: "info-line",
                    name: translate.instant("MODBUS_TCP_API_READ_WRITE.INFO_TEXT"),
                    icon: {
                        name: "information-outline",
                        size: "large",
                        color: "normal",
                    },
                },
                {
                    type: "button-line",
                    button: {
                        name: translate.instant("MODBUS_TCP_API_READ_WRITE.DOWNLOAD_PROTOCOL"),
                        callback: () => ProfileComponent.getModbusProtocol(service, translate, component.id, "tcp"),
                        icon: {
                            name: "download-outline",
                            color: "medium",
                            size: "small",
                        },
                    },
                },
                { type: "horizontal-line" },
            ],
            component: component,
        };
    }

    private static getWriteChannelLines(
        writeChannels: SharedControllerModbusTcpApiReadWrite.ModbusTcpApiChannel[],
        translate: TranslateService,
    ): OeFormlyField[] {
        const formattedWriteChannels = writeChannels.map((channel) => {
            for (const registerName in ChannelRegister) {
                if (channel.channelId.includes(registerName) && channel.channelId.startsWith("Ess0")) {
                    return `(${registerName}/${ChannelRegister[registerName]})`;
                }
            }
            return `(${channel.channelId})`;
        });

        return writeChannels.flatMap(
            (channel, i) =>
                [
                    {
                        type: "name-line",
                        name: translate.instant("MODBUS_TCP_API_READ_WRITE.REGISTER") + formattedWriteChannels[i],
                    },
                    {
                        type: "channel-line",
                        channel: channel.toString(),
                        name: translate.instant("MODBUS_TCP_API_READ_WRITE.LIMITATION"),
                        converter: Converter.POWER_IN_WATT,
                    },
                    ...(channel.translatedName(translate)
                        ? [
                              {
                                  type: "name-line",
                                  name: channel.translatedName(translate),
                                  filter: Filter.NOT_NULL_OR_UNDEFINED,
                              },
                          ]
                        : []),
                    {
                        type: "horizontal-line",
                    },
                ] as OeFormlyField[],
        );
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(this.routeService.getRouteParam<string>("componentId"));
        return ControllerModbusTcpApiDetailsComponent.getFormlyGeneralView(this.translate, component, this.service);
    }
}
