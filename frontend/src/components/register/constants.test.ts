import { describe, it, expect } from 'vitest';

import { PLANS, VALID_PLANS, PASSWORD_REQUIREMENTS } from './constants';

describe('Registration Constants', () => {
  describe('PLANS', () => {
    it('has exactly 3 plans', () => {
      expect(PLANS).toHaveLength(3);
    });

    it('contains STARTER, PRO, and ENTERPRISE plans', () => {
      const ids = PLANS.map((p) => p.id);
      expect(ids).toEqual(['STARTER', 'PRO', 'ENTERPRISE']);
    });

    it('has Starter as free plan', () => {
      const starter = PLANS.find((p) => p.id === 'STARTER')!;
      expect(starter.price).toBe('0');
      expect(starter.name).toBe('Starter');
    });

    it('has Pro as the popular plan', () => {
      const pro = PLANS.find((p) => p.id === 'PRO')!;
      expect(pro.popular).toBe(true);
    });

    it('has features array for each plan', () => {
      PLANS.forEach((plan) => {
        expect(plan.features.length).toBeGreaterThan(0);
      });
    });

    it('has daily and monthly limits for each plan', () => {
      PLANS.forEach((plan) => {
        expect(plan.limits.daily).toBeDefined();
        expect(plan.limits.monthly).toBeDefined();
      });
    });
  });

  describe('VALID_PLANS', () => {
    it('matches plan IDs from PLANS', () => {
      expect(VALID_PLANS).toEqual(['STARTER', 'PRO', 'ENTERPRISE']);
    });
  });

  describe('PASSWORD_REQUIREMENTS', () => {
    it('has 4 requirements', () => {
      expect(PASSWORD_REQUIREMENTS).toHaveLength(4);
    });

    it('validates minimum length', () => {
      const lengthReq = PASSWORD_REQUIREMENTS[0];
      expect(lengthReq.test('short')).toBe(false);
      expect(lengthReq.test('longenough')).toBe(true);
    });

    it('validates number presence', () => {
      const numberReq = PASSWORD_REQUIREMENTS[1];
      expect(numberReq.test('nodigits')).toBe(false);
      expect(numberReq.test('has1digit')).toBe(true);
    });

    it('validates uppercase letter', () => {
      const upperReq = PASSWORD_REQUIREMENTS[2];
      expect(upperReq.test('nouppercase')).toBe(false);
      expect(upperReq.test('hasUppercase')).toBe(true);
    });

    it('validates lowercase letter', () => {
      const lowerReq = PASSWORD_REQUIREMENTS[3];
      expect(lowerReq.test('NOLOWERCASE')).toBe(false);
      expect(lowerReq.test('HASLOWERCASEa')).toBe(true);
    });

    it('all pass for a valid password', () => {
      const validPassword = 'MyP@ss1word';
      PASSWORD_REQUIREMENTS.forEach((req) => {
        expect(req.test(validPassword)).toBe(true);
      });
    });

    it('reports which requirements fail for a weak password', () => {
      const weakPassword = 'abc';
      const failing = PASSWORD_REQUIREMENTS.filter((r) => !r.test(weakPassword));
      // 'abc' fails: length (< 8), number (no digit), uppercase (no uppercase) = 3 failures
      expect(failing.length).toBe(3);
    });
  });
});
