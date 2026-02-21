import { FileText } from 'lucide-react';
import { motion } from 'framer-motion';

const sections = [
  {
    title: '1. Acceptance of Terms',
    content: `By creating an account or using any FinPay services, you agree to be bound by these Terms of Service, our Privacy Policy, and any additional terms applicable to specific features. If you do not agree, do not use our services.`,
  },
  {
    title: '2. Account Registration',
    content: `You must provide accurate and complete information when creating an account. You are responsible for maintaining the confidentiality of your login credentials and for all activity that occurs under your account. You must be at least 18 years of age to use FinPay.`,
  },
  {
    title: '3. Permitted Use',
    content: `FinPay is intended for lawful financial transactions only. You agree not to use the platform for money laundering, fraud, financing of illegal activities, or any purpose that violates applicable laws and regulations.`,
  },
  {
    title: '4. Payments & Fees',
    content: `Transaction fees, conversion rates, and subscription costs are displayed before you confirm any action. FinPay reserves the right to modify pricing with 30 days' advance notice. All fees are non-refundable unless otherwise stated.`,
  },
  {
    title: '5. Intellectual Property',
    content: `All content, trademarks, logos, and software on FinPay are owned by FinPay Inc. or its licensors. You may not copy, modify, distribute, or create derivative works without explicit written permission.`,
  },
  {
    title: '6. Limitation of Liability',
    content: `FinPay is provided "as is" without warranty of any kind. To the maximum extent permitted by law, FinPay shall not be liable for indirect, incidental, or consequential damages arising from your use of the platform.`,
  },
  {
    title: '7. Account Termination',
    content: `We may suspend or terminate your account if we reasonably believe you have violated these terms, engaged in fraudulent activity, or pose a risk to our platform or other users. You may close your account at any time through settings.`,
  },
  {
    title: '8. Dispute Resolution',
    content: `Any disputes arising from these terms shall be resolved through binding arbitration in accordance with the rules of the American Arbitration Association, conducted in New York, New York.`,
  },
  {
    title: '9. Changes to Terms',
    content: `We may update these Terms of Service periodically. Material changes will be communicated via email or in-app notification at least 30 days in advance. Continued use of FinPay after changes take effect constitutes acceptance.`,
  },
];

const TermsPage = () => {
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
            <div className="w-14 h-14 mx-auto rounded-2xl bg-secondary-500/10 flex items-center justify-center mb-4">
              <FileText className="w-7 h-7 text-secondary-400" />
            </div>
            <h1 className="text-4xl sm:text-5xl font-display font-bold text-white mb-4">
              Terms of Service
            </h1>
            <p className="text-dark-400">Last updated: January 15, 2026</p>
          </div>

          <div className="card p-8 sm:p-10 space-y-8">
            <p className="text-dark-300 leading-relaxed">
              Welcome to FinPay. These Terms of Service govern your use of our platform, applications,
              and services. Please read them carefully before using FinPay.
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
                Questions about these terms? Contact us at{' '}
                <a href="mailto:legal@finpay.com" className="text-primary-400 hover:text-primary-300 transition-colors">
                  legal@finpay.com
                </a>
              </p>
            </div>
          </div>
        </motion.div>
      </section>
    </div>
  );
};

export default TermsPage;
