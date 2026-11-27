import { ChangeDetectionStrategy, Component, effect, model, OnDestroy, signal, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { InfiniteScrollCustomEvent } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject, Subscription } from "rxjs";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { GetEdgesRequest } from "src/app/shared/jsonrpc/request/getEdgesRequest";
import { User } from "src/app/shared/jsonrpc/shared";
import { Pagination } from "src/app/shared/service/pagination";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, Service, UserPermission, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { ChosenFilter, FilterComponent } from "../filter/filter.component";
import { ORDER_STATES } from "../shared/order-state";
import { SUM_STATES } from "../shared/sumState";

@Component({
    selector: "overview",
    templateUrl: "./overview.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class OverViewComponent implements OnDestroy, OnInit {
    public environment = environment;
    /** True, if there is no access to any Edge. */
    public noEdges: boolean = false;

    /** True, if the logged in user is allowed to install new edges. */
    public loggedInUserCanInstall = model<boolean>(false);

    public filteredEdges = model<Edge[]>([]);

    protected loading = signal(false);
    protected searchParams: Map<string, ChosenFilter["value"]> = new Map();
    protected isAtLeastOwner: boolean = false;
    protected filters = model<FilterComponent["allFilters"] | null>(null);

    private stopOnDestroy: Subject<void> = new Subject<void>();
    private page = 0;
    private query: string | null = null;
    private searchGeneration = 0;

    /** Limits edges in pagination response */
    private readonly limit: number = 20;
    /** True, if all available edges for this user had been retrieved */
    private limitReached: boolean = false;

    private sub: Subscription = new Subscription();

    constructor(
        public service: Service,
        public websocket: Websocket,
        public translate: TranslateService,
        public pagination: Pagination,
        protected route: ActivatedRoute,
        protected readonly router: Router,
        private userService: UserService,
    ) {
        effect(async () => {
            const user = this.userService.currentUser();
            if (user) {
                this.loggedInUserCanInstall.set(user.isAtLeast(Role.INSTALLER));
                this.isAtLeastOwner = user.isAtLeast(Role.OWNER);
                this.filters.set([
                    ...(this.isAtLeastOwner ? [ORDER_STATES(this.translate)] : []),
                    ...(this.loggedInUserCanInstall()
                        ? [environment.PRODUCT_TYPES(this.translate), SUM_STATES(this.translate)]
                        : []),
                ]);
            }
        });
    }

    /**
     * Returns the navigation tree for the overview component.
     *
     * @param user The current user
     * @param translate The translation service
     * @param service The platform service
     * @returns The navigation tree or null if device has smartphone resolution or the user does not have multiple edges
     */
    public static getNavigationTree(user: User, translate: TranslateService, service: Service): NavigationTree | null {
        if (!user?.hasMultipleEdges || service.getIsSmartphoneResolution()) {
            return null;
        }
        return new NavigationTree(
            "overview",
            { baseString: "overview" },
            { name: "apps-outline" },
            { desktop: translate.instant("MENU.OVERVIEW"), mobile: translate.instant("MENU.SYSTEMS") },
            "icon",
            [],
            null,
            { customLink: "/overview", hideFavorite: true },
        );
    }

    ngOnInit() {
        this.searchOnChange();
    }

    /**
     * Updates available edges on scroll-event
     *
     * @param infiniteScroll The InfiniteScrollCustomEvent
     */
    doInfinite(infiniteScroll: InfiniteScrollCustomEvent) {
        const searchGeneration = this.searchGeneration;
        setTimeout(() => {
            if (searchGeneration !== this.searchGeneration) {
                infiniteScroll.target.complete();
                return;
            }
            this.page++;
            this.loadNextPage()
                .then((edges) => {
                    if (searchGeneration === this.searchGeneration) {
                        this.filteredEdges.update((currentEdges) => [...currentEdges, ...edges]);
                    }
                    infiniteScroll.target.complete();
                })
                .catch(() => {
                    infiniteScroll.target.complete();
                });
        }, 200);
    }

    ngOnDestroy() {
        this.searchGeneration++;
        this.filteredEdges.set([]);
        this.sub?.unsubscribe();
        this.page = 0;
        this.limitReached = false;
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    loadNextPage(): Promise<Edge[]> {
        const searchGeneration = this.searchGeneration;
        this.loading.set(true);
        return new Promise<Edge[]>((resolve, reject) => {
            if (this.limitReached) {
                resolve([]);
                return;
            }

            const searchParamsObj: Record<string, ChosenFilter["value"]> = {};
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
            this.service
                .getEdges(req)
                .then((edges) => {
                    if (searchGeneration !== this.searchGeneration) {
                        resolve([]);
                        return;
                    }
                    this.limitReached = edges.length < this.limit;
                    const user = this.userService.currentUser();
                    // TODO could be applied before calling getEdges
                    if (user != null && !UserPermission.isUserAllowedToSeeOverview(user) && edges.length > 0) {
                        const edge = edges[0];
                        this.router.navigate(["/device", edge.id]);
                    }
                    resolve(edges);
                })
                .catch((err) => {
                    reject(err);
                });
        }).finally(() => {
            if (searchGeneration === this.searchGeneration) {
                this.loading.set(false);
            }
        });
    }

    /**
     * Search on change, triggered by searchbar input-event.
     *
     * @param event From template passed event
     */
    protected searchOnChange(searchParams?: Map<string, ChosenFilter["value"]>) {
        const searchGeneration = ++this.searchGeneration;
        if (searchParams) {
            this.searchParams = searchParams;
        }

        this.filteredEdges.set([]);

        this.page = 0;
        this.limitReached = false;

        this.loadNextPage().then((edges) => {
            if (searchGeneration === this.searchGeneration) {
                this.filteredEdges.set(edges);
                this.noEdges = edges.length === 0;
            }
        });
    }
}
