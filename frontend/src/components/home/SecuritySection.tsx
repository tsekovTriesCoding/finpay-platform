import { 
  Shield, 
  Lock, 
  Eye, 
  Key, 
  Fingerprint, 
  Server,
  CheckCircle2
} from 'lucide-react';
import { motion, useInView } from 'framer-motion';
import { useRef } from 'react';

const securityFeatures = [
  {
    icon: Lock,
    title: 'End-to-End Encryption',
    description: 'All data is encrypted using AES-256 bit encryption, both in transit and at rest.',
  },
  {
    icon: Fingerprint,
    title: 'Biometric Authentication',
    description: 'Secure access with Face ID, Touch ID, and other biometric verification methods.',
  },
  {
    icon: Key,
    title: 'Multi-Factor Auth',
    description: 'Additional security layers with SMS, email, and authenticator app verification.',
  },
  {
    icon: Eye,
    title: '24/7 Monitoring',
    description: 'Continuous security monitoring and real-time threat detection systems.',
  },
  {
    icon: Server,
    title: 'Secure Infrastructure',
    description: 'SOC 2 Type II certified data centers with redundant security measures.',
  },
  {
    icon: Shield,
    title: 'PCI DSS Compliant',
    description: 'Full compliance with Payment Card Industry Data Security Standards.',
  },
];

const certifications = [
  'SOC 2 Type II',
  'PCI DSS Level 1',
  'ISO 27001',
  'GDPR Compliant',
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const SecuritySection = () => {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });

  return (
    <section id="security" className="section-padding bg-dark-950 relative overflow-hidden">
      <div className="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-primary-500/5 to-transparent" />
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-secondary-500/10 rounded-full blur-[128px]" />

      <div className="max-w-7xl mx-auto relative" ref={ref}>
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          <motion.div
            initial={{ opacity: 0, x: -50 }}
            animate={isInView ? { opacity: 1, x: 0 } : {}}
            transition={{ duration: 0.6, ease: 'easeOut' }}
          >
            <span className="inline-block px-4 py-1.5 rounded-full bg-secondary-500/10 text-secondary-400 text-sm font-medium mb-4">
              Security
            </span>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-white mb-6">
              Bank-Grade Security for{' '}
              <span className="text-gradient">Your Peace of Mind</span>
            </h2>
            <p className="text-lg text-dark-300 mb-8">
              We take security seriously. Your financial data is protected by 
              multiple layers of enterprise-grade security, ensuring your 
              transactions and information remain safe at all times.
            </p>

            <motion.div
              className="flex flex-wrap gap-3 mb-8"
              variants={containerVariants}
              initial="hidden"
              animate={isInView ? 'visible' : 'hidden'}
            >
              {certifications.map((cert, index) => (
                <motion.div
                  key={index}
                  className="flex items-center gap-2 px-4 py-2 bg-dark-800/50 rounded-lg border border-dark-700/50"
                  variants={itemVariants}
                  whileHover={{ scale: 1.05 }}
                >
                  <CheckCircle2 className="w-4 h-4 text-secondary-500" />
                  <span className="text-sm text-dark-200">{cert}</span>
                </motion.div>
              ))}
            </motion.div>

            <motion.div
              className="card p-6 relative overflow-hidden"
              initial={{ opacity: 0, y: 20 }}
              animate={isInView ? { opacity: 1, y: 0 } : {}}
              transition={{ duration: 0.6, delay: 0.4 }}
              whileHover={{ scale: 1.02 }}
            >
              <div className="absolute inset-0 bg-gradient-to-br from-primary-500/5 to-secondary-500/5" />
              <div className="relative flex items-center gap-4">
                <motion.div
                  className="w-16 h-16 bg-gradient-to-br from-secondary-500 to-secondary-600 rounded-2xl flex items-center justify-center shadow-lg shadow-secondary-500/25"
                  animate={{ rotate: [0, 5, -5, 0] }}
                  transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
                >
                  <Shield className="w-8 h-8 text-white" />
                </motion.div>
                <div>
                  <h4 className="text-xl font-semibold text-white mb-1">$250,000</h4>
                  <p className="text-dark-400">Fraud Protection Guarantee</p>
                </div>
              </div>
            </motion.div>
          </motion.div>

          <motion.div
            className="grid grid-cols-1 sm:grid-cols-2 gap-4"
            variants={containerVariants}
            initial="hidden"
            animate={isInView ? 'visible' : 'hidden'}
          >
            {securityFeatures.map((feature, index) => (
              <motion.div
                key={index}
                className="card card-hover p-5 group"
                variants={itemVariants}
                whileHover={{ y: -5, transition: { duration: 0.2 } }}
              >
                <motion.div
                  className="w-12 h-12 rounded-xl bg-dark-800/50 flex items-center justify-center mb-4 group-hover:bg-secondary-500/10 transition-colors duration-300"
                  whileHover={{ rotate: 10 }}
                >
                  <feature.icon className="w-6 h-6 text-secondary-400" />
                </motion.div>
                <h4 className="text-lg font-semibold text-white mb-2">
                  {feature.title}
                </h4>
                <p className="text-sm text-dark-400 leading-relaxed">
                  {feature.description}
                </p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </div>
    </section>
  );
};

export default SecuritySection;
