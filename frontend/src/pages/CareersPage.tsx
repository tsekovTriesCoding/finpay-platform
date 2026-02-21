import { Briefcase, MapPin, Clock, ArrowRight, Heart, Rocket, Users, Coffee } from 'lucide-react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const openings = [
  {
    title: 'Senior Backend Engineer',
    team: 'Platform',
    location: 'New York / Remote',
    type: 'Full-time',
  },
  {
    title: 'Frontend Engineer (React)',
    team: 'Product',
    location: 'New York / Remote',
    type: 'Full-time',
  },
  {
    title: 'Staff Security Engineer',
    team: 'Security',
    location: 'New York',
    type: 'Full-time',
  },
  {
    title: 'Product Designer',
    team: 'Design',
    location: 'Remote',
    type: 'Full-time',
  },
  {
    title: 'DevOps / SRE Engineer',
    team: 'Infrastructure',
    location: 'Remote',
    type: 'Full-time',
  },
  {
    title: 'Product Manager - Payments',
    team: 'Product',
    location: 'New York / Remote',
    type: 'Full-time',
  },
];

const perks = [
  { icon: Heart, title: 'Health & Wellness', description: 'Comprehensive health, dental, and vision coverage for you and your family.' },
  { icon: Rocket, title: 'Growth Budget', description: '$2,000/year for conferences, courses, books, and professional development.' },
  { icon: Coffee, title: 'Flexible Work', description: 'Work from anywhere with flexible hours. We trust you to deliver great work.' },
  { icon: Users, title: 'Team Retreats', description: 'Quarterly team off-sites to connect, collaborate, and recharge.' },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.08, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
};

const CareersPage = () => {
  return (
    <div className="pt-24 pb-16">
      <section className="section-padding text-center">
        <motion.div
          className="max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-secondary-500/10 text-secondary-400 text-sm font-medium mb-4">
            Careers
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Build the Future of{' '}
            <span className="text-gradient">Finance</span> With Us
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Join a world-class team solving hard problems in payments, security, and infrastructure.
            We're growing fast and looking for talented people.
          </p>
        </motion.div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {perks.map((perk) => (
            <motion.div
              key={perk.title}
              className="card p-6 text-center"
              variants={itemVariants}
              whileHover={{ y: -5 }}
            >
              <div className="w-12 h-12 mx-auto rounded-xl bg-secondary-500/10 flex items-center justify-center mb-4">
                <perk.icon className="w-6 h-6 text-secondary-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">{perk.title}</h3>
              <p className="text-dark-400 text-sm leading-relaxed">{perk.description}</p>
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-3xl font-display font-bold text-white text-center mb-10"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Open Positions
        </motion.h2>
        <motion.div
          className="space-y-4"
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
        >
          {openings.map((job) => (
            <motion.div
              key={job.title}
              className="card card-hover p-5 flex flex-col sm:flex-row sm:items-center gap-4 cursor-pointer group"
              variants={itemVariants}
              whileHover={{ y: -3 }}
            >
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-white group-hover:text-gradient transition-all duration-300">
                  {job.title}
                </h3>
                <div className="flex flex-wrap items-center gap-3 mt-1.5 text-sm text-dark-400">
                  <span className="flex items-center gap-1">
                    <Briefcase className="w-3.5 h-3.5" />
                    {job.team}
                  </span>
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5" />
                    {job.location}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    {job.type}
                  </span>
                </div>
              </div>
              <ArrowRight className="w-5 h-5 text-dark-500 group-hover:text-primary-400 transition-colors shrink-0" />
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center pb-8">
        <motion.div
          className="card p-10"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <h2 className="text-2xl font-display font-bold text-white mb-4">
            Don't See the Right Role?
          </h2>
          <p className="text-dark-300 mb-6">
            We're always open to hearing from talented people. Send us your resume and we'll be in touch.
          </p>
          <Link to="/contact" className="btn-primary">
            Get in Touch
            <ArrowRight className="w-4 h-4" />
          </Link>
        </motion.div>
      </section>
    </div>
  );
};

export default CareersPage;
