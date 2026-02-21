import { Users, Target, Shield, Globe2, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const values = [
  {
    icon: Target,
    title: 'Mission-Driven',
    description: 'We believe everyone deserves access to fast, secure, and affordable financial services.',
  },
  {
    icon: Users,
    title: 'Customer First',
    description: 'Every decision we make starts with a simple question: how does this help our users?',
  },
  {
    icon: Shield,
    title: 'Trust & Security',
    description: 'We earn trust through transparency, rigorous security, and relentless integrity.',
  },
  {
    icon: Globe2,
    title: 'Global by Design',
    description: 'Built from day one to serve people in every country, every currency, every language.',
  },
];

const milestones = [
  { year: '2020', title: 'Founded', description: 'FinPay was born with a vision to democratize digital payments.' },
  { year: '2021', title: 'Series A', description: 'Raised $25M to expand our platform and engineering team.' },
  { year: '2022', title: '1M Users', description: 'Reached our first million users across 50 countries.' },
  { year: '2023', title: 'Enterprise Launch', description: 'Launched enterprise solutions for Fortune 500 companies.' },
  { year: '2024', title: 'Global Expansion', description: 'Expanded to 150+ countries with multi-currency support.' },
  { year: '2025', title: 'FinPay 2.0', description: 'Released next-gen platform with instant transfers and AI-powered fraud protection.' },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.1, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const AboutPage = () => {
  return (
    <div className="pt-24 pb-16">
      {/* Header */}
      <section className="section-padding text-center">
        <motion.div
          className="max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-secondary-500/10 text-secondary-400 text-sm font-medium mb-4">
            About Us
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Building the Future of{' '}
            <span className="text-gradient">Finance</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            FinPay is on a mission to make financial services accessible, secure,
            and effortless for everyone - from individuals to enterprises worldwide.
          </p>
        </motion.div>
      </section>

      {/* Values */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-3xl font-display font-bold text-white text-center mb-12"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
        >
          Our Core Values
        </motion.h2>
        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {values.map((value) => (
            <motion.div
              key={value.title}
              className="card card-hover p-6 text-center"
              variants={itemVariants}
              whileHover={{ y: -5 }}
            >
              <div className="w-14 h-14 mx-auto rounded-2xl bg-primary-500/10 flex items-center justify-center mb-4">
                <value.icon className="w-7 h-7 text-primary-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">{value.title}</h3>
              <p className="text-dark-400 text-sm leading-relaxed">{value.description}</p>
            </motion.div>
          ))}
        </motion.div>
      </section>

      {/* Timeline */}
      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-3xl font-display font-bold text-white text-center mb-12"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Our Journey
        </motion.h2>
        <div className="relative">
          <div className="absolute left-4 sm:left-1/2 top-0 bottom-0 w-px bg-dark-800" />
          {milestones.map((milestone, index) => (
            <motion.div
              key={milestone.year}
              className={`relative flex items-start gap-6 mb-10 ${
                index % 2 === 0 ? 'sm:flex-row' : 'sm:flex-row-reverse'
              }`}
              initial={{ opacity: 0, x: index % 2 === 0 ? -30 : 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <div className="hidden sm:block sm:w-1/2" />
              <div className="absolute left-4 sm:left-1/2 -translate-x-1/2 w-3 h-3 rounded-full bg-primary-500 border-4 border-dark-950 z-10" />
              <div className="ml-10 sm:ml-0 sm:w-1/2 card p-5">
                <span className="text-xs font-semibold text-primary-400">{milestone.year}</span>
                <h4 className="text-lg font-semibold text-white mt-1">{milestone.title}</h4>
                <p className="text-dark-400 text-sm mt-1">{milestone.description}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center pb-8">
        <motion.div
          className="card p-10"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <h2 className="text-2xl font-display font-bold text-white mb-4">
            Want to Join Our Story?
          </h2>
          <p className="text-dark-300 mb-6">
            We're always looking for talented people to help shape the future of finance.
          </p>
          <Link to="/careers" className="btn-primary">
            View Open Positions
            <ArrowRight className="w-4 h-4" />
          </Link>
        </motion.div>
      </section>
    </div>
  );
};

export default AboutPage;
