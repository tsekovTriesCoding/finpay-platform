import { ShieldCheck, CheckCircle2, ExternalLink } from 'lucide-react';
import { motion } from 'framer-motion';

const certifications = [
  {
    title: 'PCI DSS Level 1',
    description: 'The highest level of Payment Card Industry Data Security Standard certification, ensuring all cardholder data is stored, processed, and transmitted securely.',
    status: 'Certified',
  },
  {
    title: 'SOC 2 Type II',
    description: 'Independently audited controls for security, availability, processing integrity, confidentiality, and privacy of customer data.',
    status: 'Certified',
  },
  {
    title: 'ISO 27001',
    description: 'International standard for information security management systems (ISMS), demonstrating a systematic approach to managing sensitive information.',
    status: 'Certified',
  },
  {
    title: 'GDPR',
    description: 'Full compliance with the EU General Data Protection Regulation, giving European users control over their personal data.',
    status: 'Compliant',
  },
  {
    title: 'CCPA',
    description: 'Compliance with the California Consumer Privacy Act, providing California residents with enhanced data privacy rights.',
    status: 'Compliant',
  },
  {
    title: 'AML / KYC',
    description: 'Robust Anti-Money Laundering and Know Your Customer procedures to detect and prevent financial crimes.',
    status: 'Compliant',
  },
];

const practices = [
  'All data encrypted at rest (AES-256) and in transit (TLS 1.3)',
  'Multi-factor authentication enforced for all internal systems',
  'Regular third-party penetration testing and vulnerability assessments',
  'Real-time transaction monitoring with AI-powered anomaly detection',
  'Role-based access controls with principle of least privilege',
  'Comprehensive audit logging for all system actions',
  'Incident response plan tested quarterly',
  'Employee security awareness training conducted bi-annually',
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.08, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
};

const CompliancePage = () => {
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
            <ShieldCheck className="w-4 h-4 inline mr-1 -mt-0.5" />
            Compliance
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Security &{' '}
            <span className="text-gradient">Compliance</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            FinPay meets and exceeds industry standards to protect your data and
            financial transactions. Here's how we keep you safe.
          </p>
        </motion.div>
      </section>

      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-2xl font-display font-bold text-white text-center mb-10"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Certifications & Standards
        </motion.h2>
        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {certifications.map((cert) => (
            <motion.div
              key={cert.title}
              className="card card-hover p-6"
              variants={itemVariants}
              whileHover={{ y: -5 }}
            >
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-lg font-semibold text-white">{cert.title}</h3>
                <span className="text-xs px-2.5 py-1 rounded-full bg-green-400/10 text-green-400 font-medium">
                  {cert.status}
                </span>
              </div>
              <p className="text-dark-400 text-sm leading-relaxed">{cert.description}</p>
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-2xl font-display font-bold text-white text-center mb-10"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Security Practices
        </motion.h2>
        <motion.div
          className="card p-8 space-y-4"
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
        >
          {practices.map((practice) => (
            <motion.div
              key={practice}
              className="flex items-start gap-3"
              variants={itemVariants}
            >
              <CheckCircle2 className="w-5 h-5 text-secondary-500 shrink-0 mt-0.5" />
              <span className="text-dark-300 text-sm leading-relaxed">{practice}</span>
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
            Need More Details?
          </h2>
          <p className="text-dark-300 mb-6">
            For security questionnaires, audit reports, or compliance inquiries,
            contact our security team.
          </p>
          <a href="mailto:security@finpay.com" className="btn-primary inline-flex">
            <ExternalLink className="w-4 h-4" />
            security@finpay.com
          </a>
        </motion.div>
      </section>
    </div>
  );
};

export default CompliancePage;
