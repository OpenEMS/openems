export enum WidgetNature {
    'io.openems.edge.evcs.api.Evcs'
}

export enum WidgetFactory {
    'Controller.Api.ModbusTcp'
}

export class Widget {
    name: WidgetNature | WidgetFactory;
    componentId: string
}