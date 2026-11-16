import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { UserService } from "src/app/shared/service/user.service";
import { Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

export type FleetEdge = {
    id: string;
    apikey: string;
    setuppassword: string;
    comment: string;
    isOnline: boolean;
    lastmessage: string | null;
    sumState: string;
    locationId: string | null;
    groupIds: string[];
    assignedUsers: string[];
};

export type FleetLocation = {
    id: string;
    name: string;
    address: string;
    contact: string;
    notes: string;
    edgeCount: number;
};

export type FleetGroup = {
    id: string;
    name: string;
    type: "ZEV" | "vzEV" | "LEG" | "POOL" | string;
    gridPointEdgeId: string;
    notes: string;
    edgeCount: number;
};

export type FleetUser = {
    username: string;
    globalRole: string | null;
    edges: { [edgeId: string]: string };
    email: string | null;
    hasPassword: boolean;
};

export type FleetStats = {
    totalEdges: number;
    onlineEdges: number;
    totalUsers: number;
    totalLocations: number;
    totalGroups: number;
};

export type FleetData = {
    edges: FleetEdge[];
    locations: FleetLocation[];
    groups: FleetGroup[];
    users: FleetUser[];
    stats: FleetStats;
};

@Component({
    selector: "oe-admin",
    templateUrl: "./admin.component.html",
    styleUrls: ["./admin.component.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, FormsModule],
})
export class AdminComponent {

    protected readonly service = inject(Service);
    private readonly userService = inject(UserService);
    private readonly router = inject(Router);

    protected selectedTab = signal<"edges" | "users" | "locations-groups" | "onboarding">("edges");
    protected fleet = signal<FleetData | null>(null);
    protected loading = signal<boolean>(false);
    protected error = signal<string | null>(null);
    protected info = signal<string | null>(null);

    // Filter
    protected edgeSearch = signal<string>("");
    protected edgeFilter = signal<"all" | "online" | "offline">("all");

    // Formulare: Neue Anlage
    protected newEdgeId = signal("");
    protected newEdgeApikey = signal("");
    protected newEdgeComment = signal("");
    protected newEdgeLocationId = signal("");

    // Formulare: Neuer User
    protected newUsername = signal("");
    protected newPassword = signal("");
    protected newUserEmail = signal("");
    protected newUserSendWelcomeMail = signal(true);
    protected newGlobalRole = signal<string>("NONE");
    protected newUserInitialEdge = signal("");
    protected newUserInitialRole = signal("OWNER");

    // Formulare: SMTP-Konfiguration
    protected mailHost = signal("");
    protected mailPort = signal(465);
    protected mailTls = signal("ssl");
    protected mailUsername = signal("");
    protected mailPassword = signal("");
    protected mailFrom = signal("");
    protected mailTo = signal("");
    protected mailConfigured = signal(false);
    protected testMailAddress = signal("");

    // Formulare: Standort
    protected newLocName = signal("");
    protected newLocAddress = signal("");
    protected newLocContact = signal("");
    protected newLocNotes = signal("");

    // Formulare: Gruppe/Community
    protected newGrpName = signal("");
    protected newGrpType = signal("ZEV");
    protected newGrpGridPoint = signal("");
    protected newGrpNotes = signal("");

    // Onboarding Wizard
    protected obEdgeId = signal("");
    protected obComment = signal("");
    protected obLocationName = signal("");
    protected obUsername = signal("");
    protected obPassword = signal("");
    protected obConfigSnippet = signal<string | null>(null);

    // Computed Filtered Edges
    protected filteredEdges = computed(() => {
        const data = this.fleet();
        if (!data) return [];
        let list = data.edges;
        const q = this.edgeSearch().toLowerCase().trim();
        if (q) {
            list = list.filter(e =>
                e.id.toLowerCase().includes(q) ||
                (e.comment && e.comment.toLowerCase().includes(q))
            );
        }
        const filter = this.edgeFilter();
        if (filter === "online") {
            list = list.filter(e => e.isOnline);
        } else if (filter === "offline") {
            list = list.filter(e => !e.isOnline);
        }
        return list;
    });

    constructor() {
        effect(() => {
            const user = this.userService.currentUser();
            if (user != null && !user.isAtLeast(Role.ADMIN)) {
                this.router.navigate(["/overview"]);
                return;
            }
            if (user != null) {
                this.reload();
            }
        });
    }

    protected reload() {
        this.loading.set(true);
        this.error.set(null);
        this.send("neutravaAdminGetFleet", {})
            .then(r => {
                this.fleet.set(r.result as FleetData);
            })
            .catch(e => this.error.set(String(e)))
            .finally(() => this.loading.set(false));
        this.reloadMailConfig();
    }

    protected reloadMailConfig() {
        this.send("neutravaAdminGetMailConfig", {})
            .then(r => {
                const cfg = r.result as {
                    host?: string; port?: number; tls?: string; username?: string;
                    password?: string; from?: string; to?: string[]; configured?: boolean;
                };
                this.mailHost.set(cfg.host ?? "");
                this.mailPort.set(cfg.port ?? 465);
                this.mailTls.set(cfg.tls ?? "ssl");
                this.mailUsername.set(cfg.username ?? "");
                this.mailPassword.set("");
                this.mailFrom.set(cfg.from ?? "");
                this.mailTo.set((cfg.to ?? []).join(", "));
                this.mailConfigured.set(!!cfg.configured);
            })
            .catch(() => {});
    }

    protected saveMailConfig() {
        const to = this.mailTo().split(",").map(s => s.trim()).filter(s => s.length > 0);
        const params: any = {
            host: this.mailHost(),
            port: Number(this.mailPort()) || 465,
            tls: this.mailTls(),
            username: this.mailUsername(),
            from: this.mailFrom(),
            to,
        };
        if (this.mailPassword().trim()) {
            params.password = this.mailPassword().trim();
        }
        this.send("neutravaAdminSetMailConfig", params)
            .then(r => {
                const cfg = r.result as { configured?: boolean };
                this.mailConfigured.set(!!cfg.configured);
                this.mailPassword.set("");
                this.info.set("SMTP-Konfiguration gespeichert.");
            })
            .catch(e => this.error.set(String(e)));
    }

    protected sendTestMail() {
        const to = this.testMailAddress().trim();
        if (!to) return;
        this.send("neutravaAdminSendTestMail", { to })
            .then(r => {
                const res = r.result as { ok?: boolean; error?: string };
                if (res.ok) {
                    this.info.set("Testmail an " + to + " versandt.");
                } else {
                    this.error.set("Testmail fehlgeschlagen: " + (res.error ?? "unbekannt"));
                }
            })
            .catch(e => this.error.set(String(e)));
    }

    // --- Edges CRUD ---
    protected registerEdge() {
        const edgeId = this.newEdgeId().trim();
        if (!edgeId) return;
        this.send("neutravaAdminSetEdge", {
            edgeId,
            apikey: this.newEdgeApikey().trim() || edgeId,
            comment: this.newEdgeComment().trim() || edgeId,
            locationId: this.newEdgeLocationId() || null,
        }).then(r => {
            this.fleet.set(r.result as FleetData);
            this.info.set(`Anlage '${edgeId}' erfolgreich registriert.`);
            this.newEdgeId.set("");
            this.newEdgeApikey.set("");
            this.newEdgeComment.set("");
            this.newEdgeLocationId.set("");
        }).catch(e => this.error.set(String(e)));
    }

    protected deleteEdge(edgeId: string) {
        if (!confirm(`Soll Anlage '${edgeId}' wirklich gelöscht werden?`)) return;
        this.send("neutravaAdminDeleteEdge", { edgeId })
            .then(r => {
                this.fleet.set(r.result as FleetData);
                this.info.set(`Anlage '${edgeId}' gelöscht.`);
            })
            .catch(e => this.error.set(String(e)));
    }

    protected setEdgeLocation(edge: FleetEdge, locId: string) {
        this.send("neutravaAdminSetEdge", {
            edgeId: edge.id,
            locationId: locId || null,
        }).then(r => this.fleet.set(r.result as FleetData))
          .catch(e => this.error.set(String(e)));
    }

    protected toggleEdgeGroup(edge: FleetEdge, groupId: string) {
        const current = new Set(edge.groupIds || []);
        if (current.has(groupId)) {
            current.delete(groupId);
        } else {
            current.add(groupId);
        }
        this.send("neutravaAdminSetEdge", {
            edgeId: edge.id,
            groupIds: Array.from(current),
        }).then(r => this.fleet.set(r.result as FleetData))
          .catch(e => this.error.set(String(e)));
    }

    protected editEdgeComment(edge: FleetEdge) {
        const comment = prompt(`Kommentar für ${edge.id} bearbeiten:`, edge.comment);
        if (comment === null) return;
        this.send("neutravaAdminSetEdge", {
            edgeId: edge.id,
            comment,
        }).then(r => this.fleet.set(r.result as FleetData))
          .catch(e => this.error.set(String(e)));
    }

    // --- Users CRUD ---
    protected createUser() {
        const username = this.newUsername().trim();
        const password = this.newPassword().trim();
        if (!username || !password) return;

        const globalRole = this.newGlobalRole() === "ADMIN" ? "ADMIN" : null;
        const edges: { [id: string]: string } = {};
        if (this.newUserInitialEdge()) {
            edges[this.newUserInitialEdge()] = this.newUserInitialRole();
        }

        const email = this.newUserEmail().trim() || (username.includes("@") ? username : null);

        this.send("neutravaAdminSetUser", {
            username,
            password,
            email,
            sendWelcomeMail: this.newUserSendWelcomeMail(),
            globalRole,
            edges,
        }).then(() => {
            this.info.set(`Benutzer '${username}' erfolgreich angelegt.`);
            this.newUsername.set("");
            this.newPassword.set("");
            this.newUserEmail.set("");
            this.newUserInitialEdge.set("");
            this.reload();
        }).catch(e => this.error.set(String(e)));
    }

    protected deleteUser(username: string) {
        if (!confirm(`Soll Benutzer '${username}' wirklich gelöscht werden?`)) return;
        this.send("neutravaAdminDeleteUser", { username })
            .then(() => {
                this.info.set(`Benutzer '${username}' gelöscht.`);
                this.reload();
            })
            .catch(e => this.error.set(String(e)));
    }

    protected sendInvitation(username: string) {
        this.send("neutravaAdminSendInvitation", { username })
            .then(() => this.info.set(`Einladungs-E-Mail an '${username}' erfolgreich versendet.`))
            .catch(e => this.error.set(`Fehler beim Versenden der Einladung: ${e}`));
    }

    protected resetPassword(username: string) {
        const pw = prompt(`Neues Passwort für ${username}:`);
        if (!pw) return;
        this.send("neutravaAdminSetUser", { username, password: pw })
            .then(() => this.info.set(`Passwort für '${username}' aktualisiert.`))
            .catch(e => this.error.set(String(e)));
    }

    protected setUserEdgeRole(username: string, edgeId: string, role: string) {
        const user = this.fleet()?.users.find(u => u.username === username);
        if (!user) return;
        const edges = { ...user.edges, [edgeId]: role };
        this.send("neutravaAdminSetUser", { username, edges })
            .then(() => this.reload())
            .catch(e => this.error.set(String(e)));
    }

    protected removeUserEdge(username: string, edgeId: string) {
        const user = this.fleet()?.users.find(u => u.username === username);
        if (!user) return;
        const edges = { ...user.edges };
        delete edges[edgeId];
        this.send("neutravaAdminSetUser", { username, edges })
            .then(() => this.reload())
            .catch(e => this.error.set(String(e)));
    }

    // --- Locations CRUD ---
    protected createLocation() {
        const name = this.newLocName().trim();
        if (!name) return;
        this.send("neutravaAdminSetLocation", {
            name,
            address: this.newLocAddress().trim(),
            contact: this.newLocContact().trim(),
            notes: this.newLocNotes().trim(),
        }).then(r => {
            this.fleet.set(r.result as FleetData);
            this.info.set(`Standort '${name}' angelegt.`);
            this.newLocName.set("");
            this.newLocAddress.set("");
            this.newLocContact.set("");
            this.newLocNotes.set("");
        }).catch(e => this.error.set(String(e)));
    }

    protected deleteLocation(locationId: string) {
        if (!confirm(`Soll dieser Standort gelöscht werden?`)) return;
        this.send("neutravaAdminDeleteLocation", { locationId })
            .then(r => this.fleet.set(r.result as FleetData))
            .catch(e => this.error.set(String(e)));
    }

    // --- Groups / Communities CRUD ---
    protected createGroup() {
        const name = this.newGrpName().trim();
        if (!name) return;
        this.send("neutravaAdminSetGroup", {
            name,
            type: this.newGrpType(),
            gridPointEdgeId: this.newGrpGridPoint() || "",
            notes: this.newGrpNotes().trim(),
        }).then(r => {
            this.fleet.set(r.result as FleetData);
            this.info.set(`Community/Gruppe '${name}' angelegt.`);
            this.newGrpName.set("");
            this.newGrpNotes.set("");
        }).catch(e => this.error.set(String(e)));
    }

    protected deleteGroup(groupId: string) {
        if (!confirm(`Soll diese Community gelöscht werden?`)) return;
        this.send("neutravaAdminDeleteGroup", { groupId })
            .then(r => this.fleet.set(r.result as FleetData))
            .catch(e => this.error.set(String(e)));
    }

    // --- Onboarding Wizard ---
    protected runOnboarding() {
        const edgeId = this.obEdgeId().trim();
        const comment = this.obComment().trim() || edgeId;
        const username = this.obUsername().trim();
        const password = this.obPassword().trim();

        if (!edgeId || !username || !password) {
            this.error.set("Bitte Edge-ID, Benutzername und Passwort ausfüllen.");
            return;
        }

        // 1. Optional Standort anlegen wenn angegeben
        const setupFlow = async () => {
            let locId: string | null = null;
            if (this.obLocationName().trim()) {
                const locRes = await this.send("neutravaAdminSetLocation", {
                    name: this.obLocationName().trim(),
                });
                const locData = locRes.result as FleetData;
                const created = locData.locations.find(l => l.name === this.obLocationName().trim());
                if (created) locId = created.id;
            }

            // 2. Edge registrieren
            await this.send("neutravaAdminSetEdge", {
                edgeId,
                apikey: edgeId,
                comment,
                locationId: locId,
            });

            // 3. User anlegen & zuweisen
            const edges: { [id: string]: string } = {};
            edges[edgeId] = "OWNER";
            const email = username.includes("@") ? username : null;
            await this.send("neutravaAdminSetUser", {
                username,
                password,
                email,
                sendWelcomeMail: true,
                edges,
            });

            // 4. Config-Snippet generieren
            const snippet = `:org.apache.felix.configadmin.revision:=L"2"\n` +
                `apikey="${edgeId}"\n` +
                `service.pid="Controller.Api.Backend"\n` +
                `uri="wss://ems.neutrava.ch/openems-edge"`;
            this.obConfigSnippet.set(snippet);
            this.info.set(`Onboarding für '${edgeId}' & '${username}' erfolgreich abgeschlossen!`);
            this.reload();
        };

        this.loading.set(true);
        setupFlow().catch(e => this.error.set(String(e)))
                   .finally(() => this.loading.set(false));
    }

    protected copyToClipboard(text: string) {
        navigator.clipboard.writeText(text);
        this.info.set("Konfiguration in die Zwischenablage kopiert.");
    }

    protected getLocationName(id: string | null): string {
        if (!id) return "–";
        return this.fleet()?.locations.find(l => l.id === id)?.name || id;
    }

    protected getGroupName(id: string): string {
        return this.fleet()?.groups.find(g => g.id === id)?.name || id;
    }

    private send(method: string, params: {}): Promise<JsonrpcResponseSuccess> {
        this.error.set(null);
        return this.service.websocket.sendRequest(new JsonrpcRequest(method, params));
    }
}
