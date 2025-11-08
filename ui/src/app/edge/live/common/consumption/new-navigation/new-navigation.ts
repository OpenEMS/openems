import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { EvcsComponent } from "src/app/shared/components/edge/components/evcsComponent";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class CommonConsumptionHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private evcss: EvcsComponent[] = [];
    private consumptionMeters: EdgeConfig.Component[] = [];

    public static getFormlyGeneralView(config: EdgeConfig | null, translate: TranslateService, evcss: EvcsComponent[], consumptionMeters: EdgeConfig.Component[]): OeFormlyView {

        const lines: OeFormlyField[] = [];

        for (const evcs of evcss) {
            lines.push({
                type: "channel-line",
                name: evcs.alias ?? evcs.id,
                channel: evcs.powerChannel.toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }
        for (const consumptionMeter of consumptionMeters) {
            lines.push({
                type: "channel-line",
                name: consumptionMeter.alias ?? consumptionMeter.id,
                channel: ChannelAddress.fromString(consumptionMeter.id + "/ActivePower").toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        if (evcss.length !== 0 || consumptionMeters.length !== 0) {
            lines.push(
                {
                    type: "horizontal-line",
                },
                {
                    type: "value-from-channels-line",
                    name: translate.instant("General.otherConsumption"),
                    value: (currentData: CurrentData) => Converter.POSITIVE_POWER_IN_KILO_WATT(
                        Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
                    channelsToSubscribe: [
                        new ChannelAddress("_sum", "ConsumptionActivePower"),
                        ...consumptionMeters.map(el => new ChannelAddress(el.id, "ActivePower")),
                        ...evcss.map(el => el.powerChannel),
                    ],
                }
            );
        }

        return {
            title: translate.instant("General.consumption"),
            helpKey: "REDIRECT.COMMON_CONSUMPTION",
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        this.evcss = EvcsComponent.getComponents(config, edge);
        this.consumptionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

        return CommonConsumptionHomeComponent.getFormlyGeneralView(config, this.translate, this.evcss, this.consumptionMeters);
    }
}
