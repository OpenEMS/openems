import { Component, effect, ElementRef, inject, OnDestroy, ViewChild, } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { Subject } from "rxjs";
import { PlatFormService } from "src/app/platform.service";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { LayoutRefreshService } from "src/app/shared/service/layoutRefreshService";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, } from "src/app/shared/shared";
import { Widgets } from "src/app/shared/type/widgets";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
    selector: "live",
    templateUrl: "./live.component.html",
    standalone: false,
    styles: `
        @media (max-width: 576px) {
            .live-small-padding {
                padding-left: 1em;
                padding-right: 1em;
            }
        }
    `,
})
export class LiveComponent implements OnDestroy {
    @ViewChild("modal", { read: ElementRef }) public modal!: ElementRef;

    protected edge: Edge | null = null;
    protected config: EdgeConfig | null = null;
    protected widgets: Widgets | null = null;
    protected isModbusTcpWidgetAllowed: boolean = false;
    protected showRefreshDragDown: boolean = false;
    protected showNewFooter: boolean = false;
    protected isTablet: boolean = false;

    private stopOnDestroy: Subject<void> = new Subject<void>();
    private interval: ReturnType<typeof setInterval> | undefined;
    private platformService = inject(PlatFormService);

    constructor(
        private route: ActivatedRoute,
        public service: Service,
        protected utils: Utils,
        protected websocket: Websocket,
        private dataService: DataService,
        private router: Router,
        protected navigationService: NavigationService,
        private userService: UserService,
        private layoutRefresh: LayoutRefreshService,
    ) {
        this.isTablet = this.platformService.getDevice().isTablet();

        effect(() => {
            const edge = this.service.currentEdge();
            this.edge = edge;

            if (edge === null) {
                return;
            }

            this.isModbusTcpWidgetAllowed =
                EdgePermission.isModbusTcpApiWidgetAllowed(edge);

            edge?.getFirstValidConfig(websocket)?.then(async (config) => {
                this.config = config;
                this.widgets = await navigationService.getWidgets(
                    config.widgets,
                    userService.currentUser(),
                    edge,
                );
            });
            this.checkIfRefreshNeeded();
        });
    }

    public ionViewWillEnter() {
        if (this.widgets?.list) {
            this.showNewFooter =
                this.widgets?.list.filter(
                    (item) =>
                        item.name == "Evse.Controller.Single" ||
                        item.name == "Controller.IO.Heating.Room",
                )?.length > 0;
        }
        this.layoutRefresh.request(300);
    }

    ionViewWillLeave() {
        this.ngOnDestroy();
    }

    public ngOnDestroy() {
        clearInterval(this.interval);
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    protected handleRefresh: (ev: RefresherCustomEvent) => void = (
        ev: RefresherCustomEvent,
    ) => this.dataService.refresh(ev);

    protected checkIfRefreshNeeded() {
        this.interval = setInterval(async () => {
            if (this.edge?.isOnline === false) {
                this.showRefreshDragDown = false;
                return;
            }

            const lastUpdate: Date | null = this.dataService.lastUpdated();
            if (lastUpdate == null) {
                this.showRefreshDragDown = true;
                return;
            }
            this.showRefreshDragDown =
                DateTimeUtils.isDifferenceInSecondsGreaterThan(
                    20,
                    new Date(),
                    lastUpdate,
                );
        }, 5000);
    }
}
