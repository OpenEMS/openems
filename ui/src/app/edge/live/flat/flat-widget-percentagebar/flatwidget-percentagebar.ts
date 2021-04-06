import { Component, Input } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UUID } from "angular2-uuid";
import { Service, Websocket } from "src/app/shared/shared";
import { ChannelAddress } from "src/app/shared/type/channeladdress";
import { AbstractWidgetLineComponent } from "../../abstractWidgetLine.component";


@Component({
    selector: 'flat-widget-percentagebar',
    templateUrl: './flatwidget-percentagebar.html'
})


export class FlatWidgetPercentagebar extends AbstractWidgetLineComponent {


    /** value is the channel the percentagebar is reffering to */
    @Input() value: string;
    public typecheck: boolean;
    constructor(
        public route: ActivatedRoute,
        public service: Service,
        public websocket: Websocket,
    ) {
        super(websocket);
    }
    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        if (typeof (this.value) === 'string') {
            this.typecheck = true;
        } else if (typeof (this.value) === 'number') {
            this.typecheck = false;
        }
    }
    ngAfterViewInit() {
        if (typeof (this.value) === 'string') {
            this.subscribeOnChannels(UUID.UUID().toString(), [ChannelAddress.fromString(this.value)]);
        }
    }
}