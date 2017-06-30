import { routing, appRoutingProviders } from './../app.routing';
import { Device, Log, QueryReply, Data, Config, ChannelAddresses, Summary } from './device/device';
import { Dataset, EMPTY_DATASET } from './chart';
import { TemplateHelper } from './service/templatehelper';
import { WebappService, Notification } from './service/webapp.service';
import { WebsocketService, Websocket } from './service/websocket.service';

const LABELS = {
    production: "Erzeugung",
    consumption: "Verbrauch",
    consumption_warning: "Verbrauch & unbekannte Erzeuger",
    grid: "Netz",
    grid_buy: "Netzbezug",
    grid_sell: "Netzeinspeisung"
}

export { WebappService, TemplateHelper, Notification, WebsocketService, Websocket, Device, Log, Dataset, EMPTY_DATASET, Config, QueryReply, ChannelAddresses, Data, Summary, LABELS };