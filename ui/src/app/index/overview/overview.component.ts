// @ts-strict-ignore
import { Component, effect, OnDestroy } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Router } from "@angular/router";
import { InfiniteScrollCustomEvent, ViewWillEnter } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { filter, take } from "rxjs/operators";
import { GetEdgesRequest } from "src/app/shared/jsonrpc/request/getEdgesRequest";
import { Pagination } from "src/app/shared/service/pagination";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { ChosenFilter } from "../filter/FILTER.COMPONENT";

@Component({
    selector: "overview",
    templateUrl: "./OVERVIEW.COMPONENT.HTML",
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

    protected loading: boolean = false;
    protected searchParams: Map<string, ChosenFilter["value"]> = new Map();
    protected isAtLeastInstaller: boolean = false;

    private stopOnDestroy: Subject<void> = new Subject<void>();
    private page = 0;
    private query: string | null = null;

    /** Limits edges in pagination response */
    private readonly limit: number = 20;
    /** True, if all available edges for this user had been retrieved */
    private limitReached: boolean = false;

    private lastReqId: string | null = null;

    constructor(
        public service: Service,
        public websocket: Websocket,
        public utils: Utils,
        private router: Router,
        public translate: TranslateService,
        public pagination: Pagination,
        private userService: UserService,
    ) {

        effect(() => {
            const user = THIS.USER_SERVICE.CURRENT_USER();

            if (user) {
                THIS.IS_AT_LEAST_INSTALLER = USER.IS_AT_LEAST(ROLE.INSTALLER);
            }
        });
    }

    ionViewWillEnter() {
        THIS.PAGE = 0;
        THIS.FILTERED_EDGES = [];
        THIS.LIMIT_REACHED = false;
        THIS.SERVICE.METADATA.PIPE(filter(metadata => !!metadata), take(1)).subscribe(() => {
            THIS.INIT();
        });
    }

    /**
     * Updates available edges on scroll-event
     *
     * @param infiniteScroll the InfiniteScrollCustomEvent
     */
    doInfinite(infiniteScroll: InfiniteScrollCustomEvent) {
        setTimeout(() => {
            THIS.PAGE++;
            THIS.LOAD_NEXT_PAGE().then((edges) => {
                THIS.FILTERED_EDGES.PUSH(...edges);
                INFINITE_SCROLL.TARGET.COMPLETE();
            }).catch(() => {
                INFINITE_SCROLL.TARGET.COMPLETE();
            });
        }, 200);
    }

    ngOnDestroy() {
        THIS.STOP_ON_DESTROY.NEXT();
        THIS.STOP_ON_DESTROY.COMPLETE();
    }

    loadNextPage(): Promise<Edge[]> {

        THIS.LOADING = true;
        return new Promise<Edge[]>((resolve, reject) => {
            if (THIS.LIMIT_REACHED) {
                resolve([]);
                return;
            }

            const searchParamsObj = {};
            if (THIS.SEARCH_PARAMS && THIS.SEARCH_PARAMS.SIZE > 0) {
                for (const [key, value] of THIS.SEARCH_PARAMS) {
                    searchParamsObj[key] = value;
                }
            }
            const req = new GetEdgesRequest({
                page: THIS.PAGE,
                ...(THIS.QUERY && THIS.QUERY != "" && { query: THIS.QUERY }),
                ...(THIS.LIMIT && { limit: THIS.LIMIT }),
                ...(searchParamsObj && { searchParams: searchParamsObj }),
            });

            THIS.LAST_REQ_ID = REQ.ID;

            THIS.SERVICE.GET_EDGES(req)
                .then((edges) => {
                    if (THIS.LAST_REQ_ID !== REQ.ID) {
                        resolve(THIS.FILTERED_EDGES);
                    }
                    THIS.LIMIT_REACHED = EDGES.LENGTH < THIS.LIMIT;
                    resolve(edges);
                }).catch((err) => {
                    reject(err);
                });
        }).finally(() =>
            THIS.LOADING = false);
    }

    protected getAndSubscribeEdge(edge: Edge) {
        THIS.PAGINATION.GET_AND_SUBSCRIBE_EDGE(edge);
    }

    /**
     * Search on change, triggered by searchbar input-event.
     *
     * @param event from template passed event
     */
    protected searchOnChange(searchParams?: Map<string, ChosenFilter["value"]>) {

        if (searchParams) {
            THIS.SEARCH_PARAMS = searchParams;
        }

        THIS.FILTERED_EDGES = [];
        THIS.PAGE = 0;
        THIS.LIMIT_REACHED = false;

        THIS.LOAD_NEXT_PAGE().then((edges) => {
            THIS.FILTERED_EDGES = edges;
        });
    }

    private init() {

        THIS.LOAD_NEXT_PAGE().then((edges) => {
            THIS.SERVICE.METADATA
                .pipe(
                    filter(metadata => !!metadata),
                    take(1),
                )
                .subscribe(metadata => {
                    const edgeIds = OBJECT.KEYS(METADATA.EDGES);
                    THIS.NO_EDGES = EDGE_IDS.LENGTH === 0;
                    THIS.LOGGED_IN_USER_CAN_INSTALL = ROLE.IS_AT_LEAST(METADATA.USER.GLOBAL_ROLE, "installer");

                    // Forward directly to device page, if
                    // - Direct local access to Edge
                    // - No installer (I.E. guest or owner) and access to only one Edge
                    if (ENVIRONMENT.BACKEND == "OpenEMS Edge" || (!THIS.LOGGED_IN_USER_CAN_INSTALL && EDGE_IDS.LENGTH == 1)) {
                        const edge = METADATA.EDGES[edgeIds[0]];
                        setTimeout(() => {
                            THIS.ROUTER.NAVIGATE(["/device", EDGE.ID]);
                        }, 100);
                        return;
                    }
                    THIS.FILTERED_EDGES = edges;
                });
        });
    }

}
