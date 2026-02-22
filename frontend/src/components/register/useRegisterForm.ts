import { useActionState, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useAuth } from '../../contexts/AuthContext';
import { authService, type AccountPlan } from '../../api';
import { VALID_PLANS, type RegisterFormState } from './constants';

/**
 * Custom hook that encapsulates all registration business logic:
 * - Step navigation (plan selection → account details)
 * - Plan pre-selection from URL search params
 * - Form submission via React 19 `useActionState`
 * - OAuth redirect handling
 */
export function useRegisterForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { register } = useAuth();

  const urlPlan = searchParams.get('plan')?.toUpperCase() as AccountPlan | undefined;
  const initialPlan = urlPlan && VALID_PLANS.includes(urlPlan) ? urlPlan : null;

  const [step, setStep] = useState(initialPlan ? 2 : 1);
  const [selectedPlan, setSelectedPlan] = useState<AccountPlan | null>(initialPlan);

  const [formState, formAction, isPending] = useActionState(
    async (_prev: RegisterFormState, formData: FormData): Promise<RegisterFormState> => {
      if (!selectedPlan) {
        setStep(1);
        return { error: 'Please select a plan' };
      }

      const password = formData.get('password') as string;
      const confirmPassword = formData.get('confirmPassword') as string;

      const passwordValid = [
        password.length >= 8,
        /\d/.test(password),
        /[A-Z]/.test(password),
        /[a-z]/.test(password),
      ].every(Boolean);

      if (!passwordValid) {
        return { error: 'Please meet all password requirements' };
      }

      if (password !== confirmPassword) {
        return { error: 'Passwords do not match' };
      }

      // Checkboxes only appear in FormData when checked (value = 'on')
      if (formData.get('acceptTerms') !== 'on') {
        return { error: 'Please accept the terms and conditions' };
      }

      try {
        await register({
          email: formData.get('email') as string,
          password,
          firstName: formData.get('firstName') as string,
          lastName: formData.get('lastName') as string,
          phoneNumber: (formData.get('phoneNumber') as string) || undefined,
          plan: selectedPlan,
        });
        navigate('/dashboard', { replace: true });
        return { error: null };
      } catch (err: unknown) {
        if (err && typeof err === 'object' && 'response' in err) {
          const axiosError = err as { response?: { data?: { message?: string } } };
          return {
            error: axiosError.response?.data?.message || 'Registration failed. Please try again.',
          };
        }
        return { error: 'An error occurred. Please try again.' };
      }
    },
    { error: null },
  );

  const handleOAuthLogin = (provider: 'google' | 'github') => {
    window.location.href =
      provider === 'google'
        ? authService.getGoogleLoginUrl()
        : authService.getGithubLoginUrl();
  };

  return {
    step,
    setStep,
    selectedPlan,
    setSelectedPlan,
    formAction,
    formState,
    isPending,
    handleOAuthLogin,
  } as const;
}
