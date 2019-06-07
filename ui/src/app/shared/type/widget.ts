export enum WidgetNature {
    'io.openems.edge.evcs.api.Evcs',
    'io.openems.impl.controller.channelthreshold.ChannelThresholdController' // TODO deprecated
}

export enum WidgetFactory {
    'Controller.Api.ModbusTcp',
    'Controller.ChannelThreshold',
    'Controller.Io.FixDigitalOutput'
}

export class Widget {
    name: WidgetNature | WidgetFactory;
    componentId: string
}