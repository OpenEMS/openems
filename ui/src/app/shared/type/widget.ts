import { EdgeConfig } from '../edge/edgeconfig';
import { Edge } from '../edge/edge';

export enum WidgetClass {
    'Autarchy',
    'Selfconsumption',
    'Storage',
    'Grid',
    'Production',
    'Consumption',
    'Energymonitor'
}

export enum WidgetNature {
    'io.openems.edge.evcs.api.Evcs',
    'io.openems.impl.controller.channelthreshold.ChannelThresholdController', // TODO deprecated
}

export enum WidgetFactory {
    'Evcs.Cluster.SelfConsumtion',
    'Evcs.Cluster.PeakShaving',
    'Controller.Api.ModbusTcp',
    'Controller.ChannelThreshold',
    'Controller.Io.FixDigitalOutput',
    'Controller.CHP.SoC',
    'Controller.HeatingElement'
}

export class Widget {
    name: WidgetNature | WidgetFactory;
    componentId: string
}

export class Widgets {

    public static parseWidgets(edge: Edge, config: EdgeConfig): Widgets {

        let classes: WidgetClass[] = Object.values(WidgetClass) //
            .filter(v => typeof v === 'string')
            .filter(clazz => {
                if (!edge.isVersionAtLeast('2018.8')) {
                    // no filter for deprecated versions
                    return true;
                }
                switch (clazz) {
                    case 'Autarchy':
                    case 'Grid':
                        return config.hasMeter();
                    case 'Energymonitor':
                    case 'Consumption':
                        if (config.hasMeter() == true || config.hasProducer() == true || config.hasStorage() == true) {
                            return true;
                        } else {
                            return false;
                        }
                    case 'Storage':
                        return config.hasStorage();
                    case 'Production':
                    case 'Selfconsumption':
                        return config.hasProducer();
                };
                return false;
            }).map(clazz => clazz.toString());
        let list: Widget[] = [];

        for (let nature of Object.values(WidgetNature).filter(v => typeof v === 'string')) {
            for (let componentId of config.getComponentIdsImplementingNature(nature)) {
                list.push({ name: nature, componentId: componentId });
            }
        }
        for (let factory of Object.values(WidgetFactory).filter(v => typeof v === 'string')) {
            for (let componentId of config.getComponentIdsByFactory(factory)) {
                list.push({ name: factory, componentId: componentId });
            }
        }

        // explicitely sort ChannelThresholdControllers by their outputChannelAddress
        list.sort((w1, w2) => {
            const outputChannelAddress1 = config.getComponentProperties(w1.componentId)['outputChannelAddress'];
            const outputChannelAddress2 = config.getComponentProperties(w2.componentId)['outputChannelAddress'];
            if (outputChannelAddress1 && outputChannelAddress2) {
                return outputChannelAddress1.localeCompare(outputChannelAddress2);
            } else if (outputChannelAddress1) {
                return 1;
            }

            return w1.componentId.localeCompare(w1.componentId);
        });
        return new Widgets(list, classes);
    }

    /**
     * Names of Widgets.
     */
    public readonly names: string[] = [];

    private constructor(
        /**
         * List of all Widgets.
         */
        public readonly list: Widget[],
        /**
         * List of Widget-Classes.
         */
        public readonly classes: WidgetClass[]
    ) {
        // fill names
        for (let widget of list) {
            let name: string = widget.name.toString();
            if (!this.names.includes(name)) {
                this.names.push(name);
            }
        }
    }
}