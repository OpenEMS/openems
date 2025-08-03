import { Component, effect, Input, OnInit, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

@Component({
    selector: DelayedSellToGridWidgetComponent.SELECTOR,
    templateUrl: "./widget.component.html",
    standalone: false,
})
export class DelayedSellToGridWidgetComponent implements OnInit {
    service = inject(Service);
    private route = inject(ActivatedRoute);
    private userService = inject(UserService);


    private static readonly SELECTOR = "delayedSellToGridWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    /** @deprecated migration purposes*/
    protected newNavigationUrlSegment: string = "";

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        effect(() => {
            const isNewNavigation = this.userService.isNewNavigation();
            this.newNavigationUrlSegment = isNewNavigation ? "/live" : "";
        });
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }
}

