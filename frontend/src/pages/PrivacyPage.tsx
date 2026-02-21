import { Shield } from 'lucide-react';
import { motion } from 'framer-motion';

const sections = [
  {
    title: '1. Information We Collect',
    content: `We collect information you provide directly when you create an account, make transactions, or contact support. This includes your name, email address, phone number, government-issued ID (for identity verification), and financial information necessary to process payments.\n\nWe also automatically collect usage data such as IP addresses, device information, browser type, and interaction patterns to improve our services and ensure security.`,
  },
  {
    title: '2. How We Use Your Information',
    content: `Your information is used to provide and improve our services, process transactions, verify your identity, prevent fraud, comply with legal obligations, and communicate important account updates.\n\nWe may also use aggregated, anonymized data for analytics and product development. We will never sell your personal information to third parties.`,
  },
  {
    title: '3. Data Sharing & Third Parties',
    content: `We share data only when necessary: with payment processors to complete transactions, with identity verification providers, with law enforcement when legally required, and with service providers who assist in operating our platform under strict contractual obligations.`,
  },
  {
    title: '4. Data Security',
    content: `All data is encrypted using AES-256 bit encryption in transit and at rest. We maintain SOC 2 Type II certification and undergo regular third-party security audits. Access to personal data is restricted to authorized personnel on a need-to-know basis.`,
  },
  {
    title: '5. Your Rights',
    content: `Depending on your jurisdiction, you may have the right to access, correct, delete, or export your personal data. You can exercise these rights through your account settings or by contacting our privacy team at privacy@finpay.com.`,
  },
  {
    title: '6. Data Retention',
    content: `We retain your personal data for as long as your account is active or as needed to provide services. When you close your account, we delete or anonymize your data within 90 days, unless retention is required by law.`,
  },
  {
    title: '7. Changes to This Policy',
    content: `We may update this Privacy Policy from time to time. We will notify you of material changes via email or through the platform. Continued use of FinPay after changes constitutes acceptance of the updated policy.`,
  },
];

const PrivacyPage = () => {
  return (
    <div className="pt-24 pb-16">
      <section className="section-padding">
        <motion.div
          className="max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <div className="text-center mb-12">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-primary-500/10 flex items-center justify-center mb-4">
              <Shield className="w-7 h-7 text-primary-400" />
            </div>
            <h1 className="text-4xl sm:text-5xl font-display font-bold text-white mb-4">
              Privacy Policy
            </h1>
            <p className="text-dark-400">Last updated: January 15, 2026</p>
          </div>

          <div className="card p-8 sm:p-10 space-y-8">
            <p className="text-dark-300 leading-relaxed">
              At FinPay, your privacy is a top priority. This Privacy Policy explains how we collect,
              use, share, and protect your personal information when you use our platform and services.
            </p>

            {sections.map((section, index) => (
              <motion.div
                key={section.title}
                initial={{ opacity: 0, y: 15 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.05 }}
              >
                <h2 className="text-xl font-semibold text-white mb-3">{section.title}</h2>
                <p className="text-dark-400 leading-relaxed whitespace-pre-line">{section.content}</p>
              </motion.div>
            ))}

            <div className="pt-4 border-t border-dark-800/50">
              <p className="text-dark-500 text-sm">
                Questions about this policy? Contact us at{' '}
                <a href="mailto:privacy@finpay.com" className="text-primary-400 hover:text-primary-300 transition-colors">
                  privacy@finpay.com
                </a>
              </p>
            </div>
          </div>
        </motion.div>
      </section>
    </div>
  );
};

export default PrivacyPage;
