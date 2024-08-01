// @ts-strict-ignore
import { TestBed } from "@angular/core/testing";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { Service, Utils } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { SettingsComponent } from "./settings.component";
import { BehaviorSubject } from "rxjs";


describe('Edge', () => {
    const serviceSypObject = jasmine.createSpyObj<Service>('Service', ['getCurrentEdge'], {
        metadata: new BehaviorSubject({
            edges: null,
            user: { globalRole: 'admin', hasMultipleEdges: true, id: '', language: Language.DE.key, name: 'test.user', settings: {} },
        }),
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: Service, useValue: serviceSypObject },
                Utils,
            ],
        });
    });
    const settingsComponent = new SettingsComponent(Utils, serviceSypObject);

    it('+ngOnInit - Role.ADMIN', async () => {
        const result = await expectNgOnInit(serviceSypObject, Role.ADMIN, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: true,
        });
    });
    it('+ngOnInit - Role.INSTALLER', async () => {
        const result = await expectNgOnInit(serviceSypObject, Role.INSTALLER, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: false,
        });
    });
    it('+ngOnInit - Role.OWNER', async () => {
        const result = await expectNgOnInit(serviceSypObject, Role.OWNER, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: false,
            isAtLeastAdmin: false,
        });
    });

});


export async function expectNgOnInit(serviceSypObject: jasmine.SpyObj<Service>, edgeRole: Role, settingsComponent: SettingsComponent): Promise<{ isAtLeastOwner: boolean; isAtLeastInstaller: boolean; isAtLeastAdmin: boolean; }> {
    const edge = DummyConfig.dummyEdge({ role: edgeRole });
    serviceSypObject.getCurrentEdge.and.resolveTo(edge);
    serviceSypObject.metadata.next({
        edges: { [edge.id]: edge },
        user: { globalRole: 'admin', hasMultipleEdges: true, id: '', language: Language.DE.key, name: 'test.user', settings: {} },
    });
    await settingsComponent.ngOnInit();
    return {
        isAtLeastOwner: settingsComponent.isAtLeastOwner,
        isAtLeastInstaller: settingsComponent.isAtLeastInstaller,
        isAtLeastAdmin: settingsComponent.isAtLeastAdmin,
    };
}
