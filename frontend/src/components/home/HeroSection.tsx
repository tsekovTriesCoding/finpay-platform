import { ArrowRight, Play, Shield, Zap, Globe } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';

const fadeIn = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.6 } },
};

const slideUp = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: 'easeOut' } },
};

const staggerContainer = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1, delayChildren: 0.2 },
  },
};

const HeroSection = () => {
  const { isAuthenticated } = useAuth();

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
      <div className="absolute inset-0 bg-hero-pattern" />
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
      
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] opacity-20" />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20">
        <motion.div
          className="text-center max-w-4xl mx-auto"
          initial="hidden"
          animate="visible"
          variants={staggerContainer}
        >
          <motion.div
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-dark-800/50 border border-dark-700/50 backdrop-blur-sm mb-8"
            variants={fadeIn}
          >
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-secondary-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-secondary-500"></span>
            </span>
            <span className="text-sm text-dark-300">
              Introducing FinPay 2.0 - <span className="text-white font-medium">Now with instant transfers</span>
            </span>
          </motion.div>

          <motion.h1
            className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-display font-bold text-white leading-tight mb-6"
            variants={slideUp}
          >
            The Future of{' '}
            <span className="text-gradient">Payments</span>{' '}
            is Here
          </motion.h1>

          <motion.p
            className="text-lg sm:text-xl text-dark-300 max-w-2xl mx-auto mb-10"
            variants={slideUp}
          >
            Experience lightning-fast transactions, bank-grade security, and seamless 
            integration. Built for modern businesses and individuals who demand more.
          </motion.p>

          <motion.div
            className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16"
            variants={slideUp}
          >
            <Link to={isAuthenticated ? "/dashboard" : "/register"} className="btn-primary text-lg px-8 py-4 w-full sm:w-auto">
              {isAuthenticated ? 'Go to Dashboard' : 'Start Free Trial'}
              <ArrowRight className="w-5 h-5" />
            </Link>
            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Link to="/demo" className="btn-secondary text-lg px-8 py-4 w-full sm:w-auto group">
                <Play className="w-5 h-5 group-hover:text-primary-400 transition-colors" />
                Watch Demo
              </Link>
            </motion.div>
          </motion.div>

          <motion.div
            className="flex flex-wrap items-center justify-center gap-8 text-dark-400"
            variants={fadeIn}
          >
            <div className="flex items-center gap-2">
              <Shield className="w-5 h-5 text-secondary-500" />
              <span className="text-sm">Bank-grade Security</span>
            </div>
            <div className="flex items-center gap-2">
              <Zap className="w-5 h-5 text-primary-400" />
              <span className="text-sm">Instant Transfers</span>
            </div>
            <div className="flex items-center gap-2">
              <Globe className="w-5 h-5 text-secondary-500" />
              <span className="text-sm">150+ Countries</span>
            </div>
          </motion.div>
        </motion.div>

        <motion.div
          className="mt-20 relative"
          initial={{ opacity: 0, y: 60 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.5, ease: 'easeOut' }}
        >
          <div className="absolute inset-0 bg-gradient-to-t from-dark-950 via-transparent to-transparent z-10" />
          <div className="card p-2 max-w-5xl mx-auto">
            <div className="bg-dark-900 rounded-xl overflow-hidden">
              <div className="bg-dark-800/50 px-4 py-3 flex items-center gap-2 border-b border-dark-700/50">
                <div className="flex gap-2">
                  <div className="w-3 h-3 rounded-full bg-red-500/80" />
                  <div className="w-3 h-3 rounded-full bg-yellow-500/80" />
                  <div className="w-3 h-3 rounded-full bg-green-500/80" />
                </div>
                <div className="flex-1 flex justify-center">
                  <div className="bg-dark-700/50 rounded-lg px-4 py-1 text-xs text-dark-400">
                    dashboard.finpay.com
                  </div>
                </div>
              </div>
              <div className="p-6 sm:p-8">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                  {[
                    { label: 'Total Balance', value: '$124,543.00', change: '+12.5%', positive: true },
                    { label: 'Monthly Revenue', value: '$48,290.00', change: '+8.2%', positive: true },
                    { label: 'Pending', value: '$3,450.00', change: '-2.1%', positive: false },
                  ].map((stat, index) => (
                    <motion.div
                      key={index}
                      className="bg-dark-800/50 rounded-xl p-4 border border-dark-700/30"
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.5, delay: 0.7 + index * 0.1 }}
                    >
                      <p className="text-dark-400 text-sm mb-1">{stat.label}</p>
                      <p className="text-2xl font-bold text-white">{stat.value}</p>
                      <p className={`text-sm mt-1 ${stat.positive ? 'text-green-400' : 'text-red-400'}`}>
                        {stat.change} from last month
                      </p>
                    </motion.div>
                  ))}
                </div>
                <div className="bg-dark-800/30 rounded-xl h-48 flex items-center justify-center border border-dark-700/30">
                  <div className="flex items-end gap-2 h-32">
                    {[40, 65, 45, 80, 55, 90, 70, 85, 60, 95, 75, 88].map((height, i) => (
                      <motion.div
                        key={i}
                        className="w-6 sm:w-8 bg-gradient-to-t from-primary-600 to-primary-400 rounded-t-md"
                        initial={{ height: 0 }}
                        animate={{ height: `${height}%` }}
                        transition={{ duration: 0.8, delay: 1 + i * 0.05, ease: 'easeOut' }}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
};

export default HeroSection;
