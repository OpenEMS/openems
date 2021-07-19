import { Component, Input } from "@angular/core";
import { differenceInMinutes, format } from "date-fns";
import { Edge, Service, EdgeConfig, ChannelAddress } from "../../../../shared/shared";
import { EvcsComponents } from "src/app/shared/edge/edgeconfig";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ModalController, PopoverController } from "@ionic/angular";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ReceiptPopoverComponent } from "../popover/popover.component";
import { TranslateService } from "@ngx-translate/core";

@Component({
    selector: ReceiptModalComponent.SELECTOR,
    templateUrl: "./modal.component.html",
})
export class ReceiptModalComponent {
    private static readonly SELECTOR = "digitalinput-modal";

    @Input() public edge: Edge;

    public pickedComponent: string = this.translate.instant("Edge.Index.Widgets.Receipt.noComponent");
    public pickedComponentsEnergy: number | null = null;
    public startTimeframe: Date | null = null;
    public endTimeframe: Date | null = null;
    public clockTimeframe: string = '';
    public fromTime: string = "15:00";
    public toTime: string = "18:00";
    private evcsComponents: EvcsComponents[] = [];


    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    ngOnInit() {
        this.setTimeFrame();
        this.service.getConfig().then(config => {
            this.gatherEvcsComponents(config);
        })
    }

    private gatherEvcsComponents(config: EdgeConfig) {
        let evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs");
        for (let i = 0; i < evcsComponents.length; i++) {
            this.evcsComponents.push({
                icon: "car-outline",
                component: evcsComponents[i]
            })
        }
    }

    public fromTimeChanged(event) {
        this.fromTime = event.detail.value;
        this.setTimeFrame();
    }

    public toTimeChanged(event) {
        this.toTime = event.detail.value;
        this.setTimeFrame();
    }

    private setTimeFrame() {
        this.clockTimeframe = this.fromTime + ' ' + this.translate.instant("Edge.Index.Widgets.Receipt.until")
            + ' ' + this.toTime + ' ' + this.translate.instant("Edge.Index.Widgets.Receipt.oclock");
    }

    /**
     * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
     * 
     * @param fromDate             the From-Date
     * @param toDate               the To-Date
     * @param channelAddresses     the Channels to retrieve
     */
    private queryHistoricTimeseriesData(fromDate: Date, toDate: Date, channelAddresses: ChannelAddress[]): Promise<QueryHistoricTimeseriesDataResponse> {
        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses);
                edge.sendRequest(this.service.websocket, request).then(response => {
                    let result = (response as QueryHistoricTimeseriesDataResponse).result;
                    if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                        resolve(response as QueryHistoricTimeseriesDataResponse);
                    } else {
                        reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                    }
                }).catch(reason => reject(reason));
            });
        });
    }

    // picks EVCS component to gather and set data
    public async presentPopover() {
        const popover = await this.popoverCtrl.create({
            component: ReceiptPopoverComponent,
            translucent: false,
            componentProps: {
                evcsComponents: this.evcsComponents
            }
        });
        await popover.present();
        popover.onDidDismiss().then((obj) => {
            if (typeof (obj.data) !== 'undefined') {
                // set alias of component for visualization
                this.pickedComponent = obj.data.component.alias;

                // set date for receipt
                this.startTimeframe = this.service.historyPeriod.from;
                this.endTimeframe = this.service.historyPeriod.to;

                // get hours and minutes for receipt
                let fromHours = this.fromTime.split(":")[0];
                let toHours = this.toTime.split(":")[0];
                let fromMinutes = this.fromTime.split(":")[1];
                let toMinutes = this.toTime.split(":")[1];

                // set hours and minutes for receipt
                this.startTimeframe.setHours(Number(fromHours), Number(fromMinutes));
                this.endTimeframe.setHours(Number(toHours), Number(toMinutes));


                let channel = [
                    new ChannelAddress(obj.data.component.id, 'EnergySession')
                ]

                // set charging energy for visualization and receipt
                this.queryHistoricTimeseriesData(this.startTimeframe, this.endTimeframe, channel).then(response => {
                    let endIndex = 0;
                    //get correct data of charging session
                    for (let i = 0; i < response.result.timestamps.length; i++) {
                        if ((new Date(response.result.timestamps[i]).getTime() <= this.endTimeframe.getTime())
                            && (i > endIndex)) {
                            endIndex = i;
                        }
                    }
                    this.pickedComponentsEnergy = response.result.data[obj.data.component.id + '/EnergySession'][endIndex];
                })
            }
        });
    }

    public createReceipt() {
        if (this.pickedComponentsEnergy != null
            && this.pickedComponent != this.translate.instant("Edge.Index.Widgets.Receipt.noComponent")) {
            let chargeDuration = differenceInMinutes(this.endTimeframe, this.startTimeframe);
            let formatedDate = format(this.startTimeframe, "yyyy-MM-dd'T'HH:mm");
            let chargeMeter = this.pickedComponent;
            let chargeNote = "erstellt mit OpenEMS"
            window.open("https://corrently.de/service/quittung.html?tx_energy=" + (this.pickedComponentsEnergy / 1000).toString()
                + "&tx_duration=" + chargeDuration.toString() + "&tx_date=" + formatedDate.toString()
                + "&tx_meter=" + chargeMeter + "&tx_note=" + chargeNote, "_blank");
        } else {
            this.service.toast("Bitte validen Ladevorgang wählen", "danger");
        }
    }

}