import { Meter } from "../../shared/meter";
import { AbstractHomeIbn } from "./abstract-home";

export abstract class AbstractHomeGen2Ibn extends AbstractHomeIbn {

    public getGen2EnergyFlowMeterFields() {

        const standardType: string = this.translate.instant("INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.TYPE.STANDARD");
        const optionalType: string = this.translate.instant("INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.TYPE.OPTIONAL");

        const options: {
            value: Meter.GridMeterCategory;
            label: string;
        }[] = [
                {
                    value: Meter.GridMeterCategory.SMART_METER, //
                    label: Meter.toGridMeterCategoryLabelString(Meter.GridMeterCategory.SMART_METER, this.translate, optionalType),
                },
                {
                    value: Meter.GridMeterCategory.INTEGRATED_METER, //
                    label: Meter.toGridMeterCategoryLabelString(Meter.GridMeterCategory.INTEGRATED_METER, this.translate, standardType),
                },
            ];

        return options;
    }

}
