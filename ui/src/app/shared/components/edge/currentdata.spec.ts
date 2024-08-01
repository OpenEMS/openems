import { CurrentData } from './currentdata';

export function expectRatioToEqual(maxApparentPower: number | null, minDischargePower: number | null, effectivePower: number | null, result: number | null): void {
  expect(CurrentData.getEssPowerRatio(maxApparentPower, minDischargePower, effectivePower)).toEqual(result);
}

describe('CurrentData', () => {
  describe('#getEssPowerRatio', () => {

    it('should return the correct power ratio when effectivePower is positive', () => {
      const maxApparentPower = 10000;
      const minDischargePower = -5000;
      expectRatioToEqual(maxApparentPower, minDischargePower, 2500, 0.25);
    });

    it('should return the correct power ratio when effectivePower is positive - different version', () => {
      const maxApparentPower = 10000;
      const minDischargePower = -3000;
      expectRatioToEqual(maxApparentPower, minDischargePower, 1000, 0.1);
    });

    it('should return 0 when effectivePower is null', () => {
      const maxApparentPower = 10000;
      const minDischargePower = -3000;
      expectRatioToEqual(maxApparentPower, minDischargePower, null, 0);
    });

    it('should return 0 when effectivePower is 0', () => {
      const maxApparentPower = 10000;
      const minDischargePower = -3000;
      expectRatioToEqual(maxApparentPower, minDischargePower, 0, 0);
    });

    it('should handle negative effectivePower according to minDischargePower', () => {
      const maxApparentPower = 10000;
      const minDischargePower = -5000;
      expectRatioToEqual(maxApparentPower, minDischargePower, -1000, -0.2);
    });

    it('should fall back to maxApparentPower if minDischargePower is not relevant', () => {
      const maxApparentPower = 10000;
      const minDischargePower = 0;
      expectRatioToEqual(maxApparentPower, minDischargePower, -1000, 0); // Since minDischargePower is 0, we assume fall back to maxApparentPower
    });

    it('should return 0 when dividing by zero maxApparentPower', () => {
      const maxApparentPower = 0;
      const minDischargePower = -3000;
      expectRatioToEqual(maxApparentPower, minDischargePower, 1000, 0);
    });

    it('should return 0 when dividing by zero maxDischargePower with relevant edgeVersion', () => {
      const maxApparentPower = 10000;
      const minDischargePower = 0;
      expectRatioToEqual(maxApparentPower, minDischargePower, -1000, 0);
    });
  });
});
