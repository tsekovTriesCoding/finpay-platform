import { ArrowRight, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion, useInView } from 'framer-motion';
import { useRef } from 'react';

const floatingVariants = {
  animate: {
    y: [-10, 10, -10],
    transition: {
      duration: 3,
      repeat: Infinity,
      ease: 'easeInOut',
    },
  },
};

const CTASection = () => {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });

  return (
    <section className="section-padding bg-dark-950 relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-dark-900/50 to-transparent" />
      <motion.div
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary-500/10 rounded-full blur-[128px]"
        animate={{ scale: [1, 1.2, 1], opacity: [0.1, 0.15, 0.1] }}
        transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut' }}
      />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary-500/50 to-transparent" />

      <div className="max-w-4xl mx-auto relative" ref={ref}>
        <motion.div
          className="card p-8 sm:p-12 lg:p-16 text-center relative overflow-hidden"
          initial={{ opacity: 0, y: 40 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6, ease: 'easeOut' }}
        >
          <div className="absolute top-0 left-0 w-32 h-32 bg-gradient-to-br from-primary-500/20 to-transparent rounded-br-full" />
          <div className="absolute bottom-0 right-0 w-32 h-32 bg-gradient-to-tl from-secondary-500/20 to-transparent rounded-tl-full" />
          
          <motion.div
            className="absolute top-8 right-12 w-3 h-3 bg-primary-400 rounded-full opacity-60"
            variants={floatingVariants}
            animate="animate"
          />
          <motion.div
            className="absolute top-20 left-16 w-2 h-2 bg-secondary-400 rounded-full opacity-60"
            variants={floatingVariants}
            animate="animate"
            transition={{ delay: 0.5 }}
          />
          <motion.div
            className="absolute bottom-16 right-24 w-2 h-2 bg-primary-300 rounded-full opacity-60"
            variants={floatingVariants}
            animate="animate"
            transition={{ delay: 1 }}
          />

          <div className="relative">
            <motion.div
              className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-2xl mb-6 shadow-lg shadow-primary-500/25"
              initial={{ scale: 0, rotate: -180 }}
              animate={isInView ? { scale: 1, rotate: 0 } : {}}
              transition={{ type: 'spring', stiffness: 200, delay: 0.2 }}
              whileHover={{ rotate: 15, scale: 1.1 }}
            >
              <Sparkles className="w-8 h-8 text-white" />
            </motion.div>

            <motion.h2
              className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-white mb-6"
              initial={{ opacity: 0, y: 20 }}
              animate={isInView ? { opacity: 1, y: 0 } : {}}
              transition={{ duration: 0.5, delay: 0.3 }}
            >
              Ready to Transform Your{' '}
              <span className="text-gradient">Financial Future?</span>
            </motion.h2>

            <motion.p
              className="text-lg text-dark-300 max-w-2xl mx-auto mb-10"
              initial={{ opacity: 0, y: 20 }}
              animate={isInView ? { opacity: 1, y: 0 } : {}}
              transition={{ duration: 0.5, delay: 0.4 }}
            >
              Join over 2.5 million users who trust FinPay for their financial needs. 
              Start your free trial today and experience the future of payments.
            </motion.p>

            <motion.div
              className="flex flex-col sm:flex-row items-center justify-center gap-4"
              initial={{ opacity: 0, y: 20 }}
              animate={isInView ? { opacity: 1, y: 0 } : {}}
              transition={{ duration: 0.5, delay: 0.5 }}
            >
              <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                <Link to="/register" className="btn-primary text-lg px-8 py-4 w-full sm:w-auto">
                  Get Started Free
                  <ArrowRight className="w-5 h-5" />
                </Link>
              </motion.div>
              <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                <Link to="/contact" className="btn-secondary text-lg px-8 py-4 w-full sm:w-auto">
                  Talk to Sales
                </Link>
              </motion.div>
            </motion.div>

            <motion.p
              className="mt-8 text-sm text-dark-500"
              initial={{ opacity: 0 }}
              animate={isInView ? { opacity: 1 } : {}}
              transition={{ duration: 0.5, delay: 0.6 }}
            >
              No credit card required • Free 14-day trial • Cancel anytime
            </motion.p>
          </div>
        </motion.div>
      </div>
    </section>
  );
};

export default CTASection;
