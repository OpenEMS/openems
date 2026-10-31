import { SohDeterminationService } from "./soh-determination.service";

describe("SohDeterminationService", () => {
    it("resolves PREPARE_PRECHARGE from state ID 15", () => {
        const service = Object.create(SohDeterminationService.prototype) as SohDeterminationService;

        expect(service.getStateMachineName(15)).toBe("PREPARE_PRECHARGE");
    });
});
