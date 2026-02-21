import {
  CreditCard,
  Smartphone,
  Globe2,
  LineChart,
  Repeat,
  ShieldCheck,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
} from 'lucide-react';
import { Link, useParams, Navigate } from 'react-router-dom';
import { motion } from 'framer-motion';

import { useAuth } from '../contexts/AuthContext';

interface FeatureData {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  headline: string;
  description: string;
  color: string;
  highlights: string[];
}

const featureData: Record<string, FeatureData> = {
  'virtual-cards': {
    icon: CreditCard,
    title: 'Virtual & Physical Cards',
    headline: 'Issue Cards Instantly, Anywhere',
    description:
      'Create unlimited virtual cards for online purchases in seconds. Order physical cards with custom branding that are delivered worldwide. Set spending limits, freeze cards instantly, and get real-time transaction notifications.',
    color: 'from-blue-500 to-cyan-400',
    highlights: [
      'Generate virtual cards instantly for one-time or recurring use',
      'Custom-branded physical cards delivered in 5-7 business days',
      'Per-card spending limits and merchant restrictions',
      'Instant freeze/unfreeze from your dashboard or mobile app',
      'Real-time push notifications for every transaction',
      'Automatic card number rotation for enhanced security',
    ],
  },
  mobile: {
    icon: Smartphone,
    title: 'Mobile-First Experience',
    headline: 'Your Finances, Always in Your Pocket',
    description:
      'Our award-winning mobile app gives you complete control over your finances on the go. Send money, check balances, analyze spending, and manage cards — all with a beautiful, intuitive interface.',
    color: 'from-purple-500 to-pink-400',
    highlights: [
      'Native iOS and Android apps with biometric login',
      'Send and request money with just a phone number',
      'Real-time balance updates and push notifications',
      'Offline mode for viewing recent transactions',
      'Widget support for quick balance checks',
      'Dark mode and accessibility-first design',
    ],
  },
  'global-payments': {
    icon: Globe2,
    title: 'Global Payments',
    headline: 'Send Money to 150+ Countries',
    description:
      'Transfer money internationally at real exchange rates with transparent fees. Multi-currency accounts let you hold, convert, and spend in dozens of currencies without hidden markups.',
    color: 'from-emerald-500 to-teal-400',
    highlights: [
      'Transfers to 150+ countries in 50+ currencies',
      'Real mid-market exchange rates with no hidden fees',
      'Multi-currency wallets to hold and convert money',
      'SWIFT and local payment rail support',
      'Batch payments for payroll and vendor disbursements',
      'Real-time tracking from send to delivery',
    ],
  },
  analytics: {
    icon: LineChart,
    title: 'Advanced Analytics',
    headline: 'Understand Every Dollar',
    description:
      'Get real-time insights into your financial activity with powerful charts, reports, and AI-driven suggestions. Track spending patterns, set budgets, and export data for accounting.',
    color: 'from-orange-500 to-amber-400',
    highlights: [
      'Interactive dashboards with customizable widgets',
      'Automatic transaction categorization',
      'Budget tracking with overspend alerts',
      'Monthly and yearly financial summaries',
      'CSV, PDF, and API export for accounting tools',
      'AI-powered spending insights and recommendations',
    ],
  },
  recurring: {
    icon: Repeat,
    title: 'Recurring Payments',
    headline: 'Automate & Never Miss a Beat',
    description:
      'Set up recurring payments with smart scheduling so bills, subscriptions, and payroll run automatically. Get notified before each charge and easily pause or cancel anytime.',
    color: 'from-rose-500 to-red-400',
    highlights: [
      'Flexible scheduling: daily, weekly, monthly, or custom',
      'Pre-debit notifications so you are never surprised',
      'Pause, resume, or cancel with one click',
      'Smart retry logic for failed payments',
      'Calendar view of upcoming scheduled payments',
      'Bulk management for multiple recurring bills',
    ],
  },
  'fraud-protection': {
    icon: ShieldCheck,
    title: 'Fraud Protection',
    headline: 'AI-Powered Security for Every Transaction',
    description:
      'Our machine-learning fraud detection engine analyzes every transaction in real time, blocking suspicious activity before it reaches your account. Combined with instant alerts and one-tap card freezing, your money stays safe.',
    color: 'from-indigo-500 to-violet-400',
    highlights: [
      'Real-time ML-based anomaly detection on every transaction',
      'Instant alerts for suspicious or high-risk activity',
      'One-tap card freeze from app or dashboard',
      '$250,000 fraud protection guarantee',
      'Geolocation-based transaction verification',
      'Customizable risk rules and spending thresholds',
    ],
  },
};

const slugOrder = ['virtual-cards', 'mobile', 'global-payments', 'analytics', 'recurring', 'fraud-protection'];

const FeatureDetailPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { isAuthenticated } = useAuth();

  if (!slug || !featureData[slug]) {
    return <Navigate to="/" replace />;
  }

  const feature = featureData[slug];
  const currentIndex = slugOrder.indexOf(slug);
  const prevSlug = currentIndex > 0 ? slugOrder[currentIndex - 1] : null;
  const nextSlug = currentIndex < slugOrder.length - 1 ? slugOrder[currentIndex + 1] : null;

  return (
    <div className="pt-24 pb-16">
      <section className="section-padding">
        <motion.div
          className="max-w-4xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          key={slug}
        >
          <Link
            to="/#features"
            className="inline-flex items-center gap-2 text-dark-400 hover:text-white transition-colors text-sm mb-8"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Features
          </Link>

          <div className="flex items-center gap-4 mb-6">
            <div
              className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} p-0.5 shadow-lg`}
            >
              <div className="w-full h-full bg-dark-900 rounded-[14px] flex items-center justify-center">
                <feature.icon className="w-7 h-7 text-white" />
              </div>
            </div>
            <span className="text-sm font-medium text-dark-400">{feature.title}</span>
          </div>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            {feature.headline}
          </h1>

          <p className="text-lg text-dark-300 max-w-3xl leading-relaxed">
            {feature.description}
          </p>
        </motion.div>
      </section>

      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.div
          className="card p-8 sm:p-10"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          key={`${slug}-highlights`}
        >
          <h2 className="text-xl font-semibold text-white mb-6">Key Highlights</h2>
          <div className="grid sm:grid-cols-2 gap-4">
            {feature.highlights.map((item, i) => (
              <motion.div
                key={item}
                className="flex items-start gap-3"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 + i * 0.06 }}
              >
                <CheckCircle2 className="w-5 h-5 text-secondary-500 shrink-0 mt-0.5" />
                <span className="text-dark-300 text-sm leading-relaxed">{item}</span>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </section>

      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <div className="flex justify-between items-center">
          {prevSlug ? (
            <Link
              to={`/features/${prevSlug}`}
              className="btn-secondary text-sm"
            >
              <ArrowLeft className="w-4 h-4" />
              {featureData[prevSlug].title}
            </Link>
          ) : (
            <div />
          )}
          {nextSlug ? (
            <Link
              to={`/features/${nextSlug}`}
              className="btn-secondary text-sm"
            >
              {featureData[nextSlug].title}
              <ArrowRight className="w-4 h-4" />
            </Link>
          ) : (
            <div />
          )}
        </div>
      </section>

      <section className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center pb-8">
        <motion.div
          className="card p-10"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <h2 className="text-2xl font-display font-bold text-white mb-4">
            Ready to Get Started?
          </h2>
          <p className="text-dark-300 mb-6">
            Experience {feature.title.toLowerCase()} and everything else FinPay has to offer.
          </p>
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="btn-primary"
          >
            {isAuthenticated ? 'Go to Dashboard' : 'Start Free Trial'}
            <ArrowRight className="w-4 h-4" />
          </Link>
        </motion.div>
      </section>
    </div>
  );
};

export default FeatureDetailPage;
