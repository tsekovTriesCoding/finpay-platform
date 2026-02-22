import { Check } from 'lucide-react';

const STEPS = [
  { num: 1, label: 'Choose Plan' },
  { num: 2, label: 'Create Account' },
];

export default function StepIndicator({ currentStep }: { currentStep: number }) {
  return (
    <div className="flex items-center justify-center gap-3 mb-8">
      {STEPS.map((step, idx) => (
        <div key={step.num} className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-colors ${
                currentStep >= step.num
                  ? 'bg-primary-500 text-white'
                  : 'bg-dark-800 text-dark-500 border border-dark-700'
              }`}
            >
              {currentStep > step.num ? <Check className="w-4 h-4" /> : step.num}
            </div>
            <span
              className={`text-sm font-medium transition-colors ${
                currentStep >= step.num ? 'text-white' : 'text-dark-500'
              }`}
            >
              {step.label}
            </span>
          </div>
          {idx < STEPS.length - 1 && (
            <div
              className={`w-12 h-0.5 transition-colors ${
                currentStep > step.num ? 'bg-primary-500' : 'bg-dark-700'
              }`}
            />
          )}
        </div>
      ))}
    </div>
  );
}
