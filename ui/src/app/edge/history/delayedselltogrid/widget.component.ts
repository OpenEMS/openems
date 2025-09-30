import { Component, effect, Input, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

@Component({
    selector: DELAYED_SELL_TO_GRID_WIDGET_COMPONENT.SELECTOR,
    templateUrl: "./WIDGET.COMPONENT.HTML",
    standalone: false,
})
export class DelayedSellToGridWidgetComponent implements OnInit {

    private static readonly SELECTOR = "delayedSellToGridWidget";
    @Input({ required: true }) public period!: DEFAULT_TYPES.HISTORY_PERIOD;
    @Input({ required: true }) public componentId!: string;

    public edge: Edge | null = null;
    public component: EDGE_CONFIG.COMPONENT | null = null;

    /** @deprecated migration purposes*/
    protected newNavigationUrlSegment: string = "";

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        private userService: UserService,
    ) {
        effect(() => {
            const isNewNavigation = THIS.USER_SERVICE.IS_NEW_NAVIGATION();
            THIS.NEW_NAVIGATION_URL_SEGMENT = isNewNavigation ? "/live" : "";
        });
    }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
            THIS.SERVICE.GET_CONFIG().then(config => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.COMPONENT_ID);
            });
        });
    }
}

