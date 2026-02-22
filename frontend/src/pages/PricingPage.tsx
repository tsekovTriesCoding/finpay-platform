import { Check, ArrowRight, Sparkles, Zap, Building2, Settings } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

import { useAuth } from '../contexts/AuthContext';
import type { AccountPlan } from '../api/authApi';

const plans = [
  {
    name: 'Starter',
    icon: Zap,
    price: '0',
    period: 'Free forever',
    description: 'Perfect for individuals getting started with digital payments.',
    features: [
      'Up to 10 transactions/month',
      '1 virtual card',
      'Basic analytics dashboard',
      'Email support',
      'Mobile app access',
    ],
    cta: 'Get Started Free',
    popular: false,
    gradient: 'from-dark-700 to-dark-800',
  },
  {
    name: 'Pro',
    icon: Sparkles,
    price: '29',
    period: '/month',
    description: 'For growing businesses that need more power and flexibility.',
    features: [
      'Unlimited transactions',
      '10 virtual cards',
      'Advanced analytics & reports',
      'Priority email & chat support',
      'Multi-currency accounts',
      'Recurring payments',
      'API access',
    ],
    cta: 'Start Free Trial',
    popular: true,
    gradient: 'from-primary-600 to-primary-500',
  },
  {
    name: 'Enterprise',
    icon: Building2,
    price: 'Custom',
    period: 'Contact us',
    description: 'Tailored solutions for large organizations with complex needs.',
    features: [
      'Everything in Pro',
      'Unlimited virtual & physical cards',
      'Custom integrations',
      'Dedicated account manager',
      'SLA guarantee (99.99%)',
      'Advanced fraud protection',
      'Custom compliance reports',
      'On-premise deployment option',
    ],
    cta: 'Talk to Sales',
    popular: false,
    gradient: 'from-secondary-600 to-secondary-500',
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.15, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const PricingPage = () => {
  const { isAuthenticated, user } = useAuth();

  const currentPlan = user?.plan ?? 'STARTER';
  const planOrder: AccountPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];
  const currentPlanIndex = planOrder.indexOf(currentPlan);

  return (
    <div className="pt-24 pb-16">
      <section className="section-padding text-center">
        <motion.div
          className="max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-primary-500/10 text-primary-400 text-sm font-medium mb-4">
            Pricing
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Simple, Transparent{' '}
            <span className="text-gradient">Pricing</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Choose the plan that fits your needs. No hidden fees, no surprises.
            Start free and scale as you grow.
          </p>
        </motion.div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.div
          className="grid grid-cols-1 md:grid-cols-3 gap-8"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {plans.map((plan) => (
            <motion.div
              key={plan.name}
              className={`card p-8 relative flex flex-col ${
                plan.popular ? 'border-primary-500/50 shadow-2xl shadow-primary-500/10' : ''
              }`}
              variants={itemVariants}
              whileHover={{ y: -8, transition: { duration: 0.3 } }}
            >
              {plan.popular && (
                <div className="absolute -top-4 left-1/2 -translate-x-1/2">
                  <span className="px-4 py-1.5 rounded-full bg-gradient-to-r from-primary-600 to-primary-500 text-white text-sm font-medium shadow-lg shadow-primary-500/25">
                    Most Popular
                  </span>
                </div>
              )}

              <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${plan.gradient} flex items-center justify-center mb-6`}>
                <plan.icon className="w-6 h-6 text-white" />
              </div>

              <h3 className="text-2xl font-bold text-white mb-2">{plan.name}</h3>
              <p className="text-dark-400 text-sm mb-6">{plan.description}</p>

              <div className="mb-8">
                {plan.price === 'Custom' ? (
                  <span className="text-4xl font-display font-bold text-white">Custom</span>
                ) : (
                  <>
                    <span className="text-4xl font-display font-bold text-white">${plan.price}</span>
                    <span className="text-dark-400 ml-1">{plan.period}</span>
                  </>
                )}
              </div>

              <ul className="space-y-3 mb-8 flex-1">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-3">
                    <Check className="w-5 h-5 text-secondary-500 shrink-0 mt-0.5" />
                    <span className="text-dark-300 text-sm">{feature}</span>
                  </li>
                ))}
              </ul>

              {(() => {
                const planKey = plan.name.toUpperCase() as AccountPlan;
                const thisPlanIndex = planOrder.indexOf(planKey);

                if (isAuthenticated && thisPlanIndex <= currentPlanIndex) {
                  return (
                    <button
                      disabled
                      className="btn-secondary w-full opacity-60 cursor-not-allowed"
                    >
                      {thisPlanIndex === currentPlanIndex ? 'Current Plan' : 'Included'}
                    </button>
                  );
                }

                if (isAuthenticated && thisPlanIndex > currentPlanIndex) {
                  return (
                    <Link
                      to="/settings?tab=plan"
                      className={`${plan.popular ? 'btn-primary' : 'btn-secondary'} w-full`}
                    >
                      <Settings className="w-4 h-4" />
                      Manage Plan
                    </Link>
                  );
                }

                return (
                  <Link
                    to={`/register?plan=${plan.name.toUpperCase()}`}
                    className={`${plan.popular ? 'btn-primary' : 'btn-secondary'} w-full`}
                  >
                    {plan.cta}
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                );
              })()}
            </motion.div>
          ))}
        </motion.div>
      </section>

    </div>
  );
};

export default PricingPage;
