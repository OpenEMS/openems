import { Component, Input, OnInit } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { MeterComponentsModule } from "src/app/shared/components/edge/meter/meter.module";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { Converter } from "src/app/shared/components/shared/converter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { HistoryUtils } from "src/app/shared/utils/utils";

@Component({
    selector: "common-storage-system",
    templateUrl: "./storage-system.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ModalComponentsModule,
        ComponentsBaseModule,
        MeterComponentsModule,
        LocaleProvider,
        LiveDataServiceProvider,
    ],
})
export class StorageSystemComponent implements OnInit {

    @Input({ required: true }) public component: EdgeConfig.Component | null = null;

    protected readonly HistoryUtils = HistoryUtils;
    protected readonly Converter = Converter;

    protected batteryInverter: EdgeConfig.Component | null = null;
    protected hasMultipleEss: boolean = false;
    private edge: Edge | null = null;

    constructor(
        private service: Service,
    ) { }

    async ngOnInit(): Promise<void> {
        this.edge = await this.service.getCurrentEdge();
        const config = this.edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        AssertionUtils.assertIsDefined(this.component);
        this.batteryInverter = config.getComponentFromOtherComponentsProperty(this.component.id, "batteryInverter.id");
        this.hasMultipleEss = config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"))?.length > 1;
    }

    public readonly CHARGE_POWER_FORMATTED = (value: any): string => {
        const v = this.HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(value);
        return v == null ? "-" : Formatter.FORMAT_WATT(v);
    };

    public readonly DISCHARGE_POWER_FORMATTED = (value: any): string => {
        const v = this.HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value);
        return v == null ? "-" : Formatter.FORMAT_WATT(v);
    };
}
