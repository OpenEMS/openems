// @ts-strict-ignore
import { Component, effect, OnDestroy, signal } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { InfiniteScrollCustomEvent, Platform, ViewWillEnter } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject, Subscription } from "rxjs";
import { GetEdgesRequest } from "src/app/shared/jsonrpc/request/getEdgesRequest";
import { Pagination } from "src/app/shared/service/pagination";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { ChosenFilter, FilterComponent } from "../filter/filter.component";
import { ORDER_STATES } from "../shared/order-state";
import { SUM_STATES } from "../shared/sumState";

@Component({
    selector: "overview",
    templateUrl: "./overview.component.html",
    standalone: false,
})
export class OverViewComponent implements ViewWillEnter, OnDestroy {
    public environment = environment;
    /** True, if there is no access to any Edge. */
    public noEdges: boolean = false;

    /**  True, if the logged in user is allowed to install new edges. */
    public loggedInUserCanInstall: boolean = false;

    public form: FormGroup;
    public filteredEdges: Edge[] = [];

    protected loading = signal(false);
    protected searchParams: Map<string, ChosenFilter["value"]> = new Map();
    protected isAtLeastOwner: boolean = false;
    protected filters: FilterComponent["allFilters"] | null = null;

    private stopOnDestroy: Subject<void> = new Subject<void>();
    private page = 0;
    private query: string | null = null;

    /** Limits edges in pagination response */
    private readonly limit: number = 20;
    /** True, if all available edges for this user had been retrieved */
    private limitReached: boolean = false;

    private lastReqId: string | null = null;
    private sub: Subscription = new Subscription();

    constructor(
        public service: Service,
        public websocket: Websocket,
        public utils: Utils,
        public translate: TranslateService,
        public pagination: Pagination,
        protected route: ActivatedRoute,
        private router: Router,
        private userService: UserService,
        private platform: Platform,
    ) {

        effect(() => {
            const user = this.userService.currentUser();
            if (user) {
                this.loggedInUserCanInstall = user.isAtLeast(Role.INSTALLER);
                this.isAtLeastOwner = user.isAtLeast(Role.OWNER);

                this.filters = [
                    ...(this.isAtLeastOwner ? [ORDER_STATES(this.translate)] : []),
                    ...(this.loggedInUserCanInstall ? [environment.PRODUCT_TYPES(this.translate), SUM_STATES(this.translate)] : []),
                ];
                this.loadNextPage();
            }
        });
    }

    ionViewWillEnter() {
        this.page = 0;
        this.filteredEdges = [];
        this.limitReached = false;
        this.filters = [
            ...(this.isAtLeastOwner ? [ORDER_STATES(this.translate)] : []),
            ...(this.loggedInUserCanInstall ? [environment.PRODUCT_TYPES(this.translate), SUM_STATES(this.translate)] : []),
        ];
    }

    ionViewDidEnter() {
        // TODO implement gestures
        // prevent url segment pop by back navigation gesture
        this.sub = this.platform.backButton.subscribeWithPriority(1, () => { });
    }

    ionViewWillLeave() {
        this.filters = [];
        this.sub?.unsubscribe();
        this.ngOnDestroy();
    }

    /**
     * Updates available edges on scroll-event
     *
     * @param infiniteScroll the InfiniteScrollCustomEvent
     */
    doInfinite(infiniteScroll: InfiniteScrollCustomEvent) {
        setTimeout(() => {
            this.page++;
            this.loadNextPage().then((edges) => {
                this.filteredEdges.push(...edges);
                infiniteScroll.target.complete();
            }).catch(() => {
                infiniteScroll.target.complete();
            });
        }, 200);
    }

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    loadNextPage(): Promise<Edge[]> {

        this.loading.set(true);
        return new Promise<Edge[]>((resolve, reject) => {
            if (this.limitReached) {
                resolve([]);
                return;
            }

            const searchParamsObj = {};
            if (this.searchParams && this.searchParams.size > 0) {
                for (const [key, value] of this.searchParams) {
                    searchParamsObj[key] = value;
                }
            }
            const req = new GetEdgesRequest({
                page: this.page,
                ...(this.query && this.query != "" && { query: this.query }),
                ...(this.limit && { limit: this.limit }),
                ...(searchParamsObj && { searchParams: searchParamsObj }),
            });

            this.lastReqId = req.id;

            this.service.getEdges(req)
                .then((edges) => {
                    if (this.lastReqId !== req.id) {
                        resolve(this.filteredEdges);
                    }
                    this.limitReached = edges.length < this.limit;
                    const user = this.userService.currentUser();
                    if (environment.backend == "OpenEMS Edge" && user.hasMultipleEdges === false || (Role.isAtMost(user.globalRole, Role.OWNER) && edges.length == 1)) {
                        const edge = edges[0];
                        setTimeout(() => {
                            this.router.navigate(["/device", edge.id]);
                        }, 100);
                    }
                    resolve(edges);
                }).catch((err) => {
                    reject(err);
                });
        }).finally(() =>
            this.loading.set(false));
    }

    protected getAndSubscribeEdge(edge: Edge) {
        this.pagination.getAndSubscribeEdge(edge);
    }

    /**
     * Search on change, triggered by searchbar input-event.
     *
     * @param event from template passed event
     */
    protected searchOnChange(searchParams?: Map<string, ChosenFilter["value"]>) {

        if (searchParams) {
            this.searchParams = searchParams;
        }

        this.filteredEdges = [];
        this.page = 0;
        this.limitReached = false;

        this.loadNextPage().then((edges) => {
            this.filteredEdges = edges;
        });
    }
}
