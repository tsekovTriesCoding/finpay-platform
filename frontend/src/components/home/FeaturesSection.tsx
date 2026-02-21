import { 
  CreditCard, 
  Smartphone, 
  Globe2, 
  LineChart, 
  Repeat, 
  ShieldCheck,
  ArrowRight
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useInView } from 'framer-motion';
import { useRef } from 'react';

const features = [
  {
    icon: CreditCard,
    title: 'Virtual & Physical Cards',
    description: 'Issue unlimited virtual cards instantly. Physical cards delivered worldwide with custom branding options.',
    color: 'from-blue-500 to-cyan-400',
    slug: 'virtual-cards',
  },
  {
    icon: Smartphone,
    title: 'Mobile-First Experience',
    description: 'Manage your finances on the go with our award-winning mobile app. Available on iOS and Android.',
    color: 'from-purple-500 to-pink-400',
    slug: 'mobile',
  },
  {
    icon: Globe2,
    title: 'Global Payments',
    description: 'Send and receive money in 150+ countries. Multi-currency accounts with real exchange rates.',
    color: 'from-emerald-500 to-teal-400',
    slug: 'global-payments',
  },
  {
    icon: LineChart,
    title: 'Advanced Analytics',
    description: 'Real-time insights and spending analytics. Track every transaction with detailed reports.',
    color: 'from-orange-500 to-amber-400',
    slug: 'analytics',
  },
  {
    icon: Repeat,
    title: 'Recurring Payments',
    description: 'Automate your payments with smart scheduling. Never miss a bill or subscription again.',
    color: 'from-rose-500 to-red-400',
    slug: 'recurring',
  },
  {
    icon: ShieldCheck,
    title: 'Fraud Protection',
    description: 'AI-powered fraud detection protects every transaction. Real-time alerts and instant card freezing.',
    color: 'from-indigo-500 to-violet-400',
    slug: 'fraud-protection',
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1, delayChildren: 0.2 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const FeaturesSection = () => {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });

  return (
    <section id="features" className="section-padding bg-dark-950 relative overflow-hidden">
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-primary-500/5 rounded-full blur-[128px]" />
      
      <div className="max-w-7xl mx-auto relative" ref={ref}>
        <motion.div
          className="text-center max-w-3xl mx-auto mb-16"
          initial={{ opacity: 0, y: 20 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6 }}
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-primary-500/10 text-primary-400 text-sm font-medium mb-4">
            Features
          </span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-white mb-6">
            Everything You Need to{' '}
            <span className="text-gradient">Manage Money</span>
          </h2>
          <p className="text-lg text-dark-300">
            From everyday transactions to complex financial operations, 
            FinPay provides all the tools you need in one powerful platform.
          </p>
        </motion.div>

        <motion.div
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8"
          variants={containerVariants}
          initial="hidden"
          animate={isInView ? 'visible' : 'hidden'}
        >
          {features.map((feature, index) => (
            <motion.div
              key={index}
              className="card card-hover p-6 lg:p-8 group"
              variants={itemVariants}
              whileHover={{ y: -8, transition: { duration: 0.3 } }}
            >
              <motion.div
                className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} p-0.5 mb-6 shadow-lg`}
                whileHover={{ scale: 1.1, rotate: 5 }}
                transition={{ type: 'spring', stiffness: 300 }}
              >
                <div className="w-full h-full bg-dark-900 rounded-[14px] flex items-center justify-center">
                  <feature.icon className="w-6 h-6 text-white" />
                </div>
              </motion.div>

              <h3 className="text-xl font-semibold text-white mb-3 group-hover:text-gradient transition-all duration-300">
                {feature.title}
              </h3>
              <p className="text-dark-400 mb-4 leading-relaxed">
                {feature.description}
              </p>

              <motion.div whileHover={{ x: 5 }}>
                <Link
                  to={`/features/${feature.slug}`}
                  className="inline-flex items-center gap-2 text-primary-400 font-medium text-sm"
                >
                  Learn more
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </motion.div>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
};

export default FeaturesSection;
