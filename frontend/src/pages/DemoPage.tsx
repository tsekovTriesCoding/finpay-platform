import { Play, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

import { useAuth } from '../contexts/AuthContext';

const demoSteps = [
  {
    title: 'Create Your Account',
    description: 'Sign up in seconds with just your email. No credit card required — your free trial starts immediately.',
    time: '0:00',
  },
  {
    title: 'Fund Your Wallet',
    description: 'Add funds via bank transfer, card, or crypto. Multi-currency support available from day one.',
    time: '0:45',
  },
  {
    title: 'Send & Receive Payments',
    description: 'Initiate instant transfers to anyone, anywhere. Track every transaction in real-time from your dashboard.',
    time: '1:30',
  },
  {
    title: 'Manage & Analyze',
    description: 'Use advanced analytics to understand your spending, set budgets, and generate compliance reports.',
    time: '2:15',
  },
];

const DemoPage = () => {
  const { isAuthenticated } = useAuth();

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
            Demo
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            See FinPay in <span className="text-gradient">Action</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Watch how easy it is to manage your finances with FinPay.
            From sign-up to your first transaction in under 3 minutes.
          </p>
        </motion.div>
      </section>
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.div
          className="card p-2 relative overflow-hidden group cursor-pointer"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          whileHover={{ scale: 1.01 }}
        >
          <div className="bg-dark-900 rounded-xl aspect-video flex items-center justify-center relative">
            <div className="absolute inset-0 bg-gradient-to-br from-primary-500/10 to-secondary-500/10" />
            <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:3rem_3rem] opacity-20" />
            <motion.div
              className="relative w-20 h-20 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center shadow-2xl shadow-primary-500/30"
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.95 }}
            >
              <Play className="w-8 h-8 text-white ml-1" />
            </motion.div>
            <span className="absolute bottom-6 right-6 text-sm text-dark-400 bg-dark-900/80 px-3 py-1 rounded-lg backdrop-blur-sm">
              3:00
            </span>
          </div>
        </motion.div>
      </section>

      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-3xl font-display font-bold text-white text-center mb-12"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          What You'll See
        </motion.h2>
        <div className="space-y-6">
          {demoSteps.map((step, index) => (
            <motion.div
              key={step.title}
              className="card card-hover p-6 flex items-start gap-5"
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
            >
              <div className="shrink-0 w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center text-primary-400 font-bold text-sm">
                {index + 1}
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-white mb-1">{step.title}</h3>
                <p className="text-dark-400 text-sm leading-relaxed">{step.description}</p>
              </div>
              <span className="text-xs text-dark-500 shrink-0 mt-1">{step.time}</span>
            </motion.div>
          ))}
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
            Ready to Try It Yourself?
          </h2>
          <p className="text-dark-300 mb-6">
            Create your free account and experience everything you just saw - and more.
          </p>
          <Link to={isAuthenticated ? '/dashboard' : '/register'} className="btn-primary">
            {isAuthenticated ? 'Go to Dashboard' : 'Start Free Trial'}
            <ArrowRight className="w-4 h-4" />
          </Link>
        </motion.div>
      </section>
    </div>
  );
};

export default DemoPage;
