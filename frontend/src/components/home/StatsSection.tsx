import { useRef } from 'react';
import { motion, useInView, useSpring, useTransform } from 'framer-motion';
import { useEffect } from 'react';

const stats = [
  { value: 2.5, suffix: 'M+', label: 'Active Users', prefix: '' },
  { value: 150, suffix: '+', label: 'Countries Supported', prefix: '' },
  { value: 99.99, suffix: '%', label: 'Uptime Guarantee', prefix: '' },
  { value: 50, suffix: 'B+', label: 'Transactions Processed', prefix: '$' },
];

const AnimatedCounter = ({ 
  value, 
  suffix, 
  prefix, 
  isInView 
}: { 
  value: number; 
  suffix: string; 
  prefix: string;
  isInView: boolean;
}) => {
  const spring = useSpring(0, { duration: 2000 });
  const display = useTransform(spring, (current) => 
    value % 1 !== 0 ? current.toFixed(2) : Math.floor(current).toString()
  );

  useEffect(() => {
    if (isInView) {
      spring.set(value);
    }
  }, [isInView, spring, value]);

  return (
    <span>
      {prefix}
      <motion.span>{display}</motion.span>
      {suffix}
    </span>
  );
};

const StatsSection = () => {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  return (
    <section className="py-16 lg:py-20 bg-dark-900/50 border-y border-dark-800/50 relative overflow-hidden">
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary-500/50 to-transparent" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-secondary-500/50 to-transparent" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" ref={ref}>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12">
          {stats.map((stat, index) => (
            <motion.div
              key={index}
              className="text-center group"
              initial={{ opacity: 0, y: 20 }}
              animate={isInView ? { opacity: 1, y: 0 } : {}}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <motion.div
                className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-gradient mb-2"
                whileHover={{ scale: 1.05 }}
              >
                <AnimatedCounter 
                  value={stat.value} 
                  suffix={stat.suffix} 
                  prefix={stat.prefix}
                  isInView={isInView}
                />
              </motion.div>
              <p className="text-dark-400 text-sm sm:text-base">{stat.label}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default StatsSection;
