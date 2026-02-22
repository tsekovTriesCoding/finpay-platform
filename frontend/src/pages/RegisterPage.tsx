import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

import { PLANS, StepIndicator, PlanSelectionStep, AccountDetailsStep, useRegisterForm } from '../components/register';

export default function RegisterPage() {
  const {
    step,
    setStep,
    selectedPlan,
    setSelectedPlan,
    formAction,
    formState,
    handleOAuthLogin,
  } = useRegisterForm();

  const activePlan = PLANS.find((p) => p.id === selectedPlan);

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-950 px-4 py-12 relative overflow-hidden">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:4rem_4rem] opacity-20" />

      <motion.div
        className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary-500/20 rounded-full blur-[128px]"
        animate={{ scale: [1, 1.1, 1], opacity: [0.2, 0.3, 0.2] }}
        transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
      />
      <motion.div
        className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-secondary-500/20 rounded-full blur-[128px]"
        animate={{ scale: [1.1, 1, 1.1], opacity: [0.3, 0.2, 0.3] }}
        transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
      />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-lg relative z-10"
      >
        <div className="card p-8">
          <div className="text-center mb-6">
            <Link to="/" className="inline-flex items-center gap-2 mb-4">
              <div className="w-10 h-10 bg-gradient-to-br from-primary-600 to-primary-500 rounded-xl flex items-center justify-center shadow-lg shadow-primary-500/25">
                <span className="text-white font-bold text-lg">F</span>
              </div>
              <span className="text-2xl font-bold text-gradient">FinPay</span>
            </Link>
            <h1 className="text-2xl font-bold text-white">Create your account</h1>
            <p className="text-dark-400 mt-2">Start managing your finances today</p>
          </div>

          <StepIndicator currentStep={step} />

          {step === 2 && activePlan && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-4 flex items-center justify-center"
            >
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-dark-800/80 border border-dark-700/50">
                <activePlan.icon className="w-4 h-4 text-primary-400" />
                <span className="text-sm text-dark-300">
                  <span className="text-white font-medium">{activePlan.name}</span> plan
                  {activePlan.price !== 'Custom' && (
                    <span className="text-dark-500 ml-1">
                      - ${activePlan.price}{activePlan.period}
                    </span>
                  )}
                </span>
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="text-xs text-primary-400 hover:text-primary-300 ml-1"
                >
                  Change
                </button>
              </div>
            </motion.div>
          )}

          <AnimatePresence mode="wait">
            {step === 1 ? (
              <PlanSelectionStep
                selectedPlan={selectedPlan}
                onSelect={setSelectedPlan}
                onContinue={() => setStep(2)}
              />
            ) : (
              <AccountDetailsStep
                error={formState.error}
                formAction={formAction}
                onBack={() => setStep(1)}
                handleOAuthLogin={handleOAuthLogin}
              />
            )}
          </AnimatePresence>

          <p className="mt-6 text-center text-sm text-dark-400">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary-400 hover:text-primary-300">
              Sign in
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
