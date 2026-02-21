import { Cookie } from 'lucide-react';
import { motion } from 'framer-motion';

const sections = [
  {
    title: '1. What Are Cookies?',
    content: `Cookies are small text files stored on your device when you visit a website. They help us remember your preferences, understand how you use our platform, and improve your experience.`,
  },
  {
    title: '2. Essential Cookies',
    content: `These cookies are strictly necessary for FinPay to function. They handle authentication, security tokens, session management, and fraud prevention. You cannot opt out of essential cookies as they are required for the platform to operate.`,
  },
  {
    title: '3. Analytics Cookies',
    content: `We use analytics cookies to understand how visitors interact with our platform. This includes page visits, navigation patterns, and feature usage. This data is aggregated and anonymized. You can opt out of analytics cookies through your browser settings.`,
  },
  {
    title: '4. Preference Cookies',
    content: `Preference cookies remember settings like your language, currency, and display preferences so you don't have to reconfigure them each visit.`,
  },
  {
    title: '5. Managing Cookies',
    content: `Most browsers allow you to control cookies through their settings. You can delete existing cookies and configure your browser to block future cookies. Note that disabling certain cookies may limit functionality on FinPay.\n\nTo manage cookies, check your browser's help documentation or settings menu.`,
  },
  {
    title: '6. Third-Party Cookies',
    content: `Some third-party services integrated into FinPay (such as analytics providers) may set their own cookies. We do not control these cookies and recommend reviewing the privacy policies of those services.`,
  },
  {
    title: '7. Updates to This Policy',
    content: `We may update this Cookie Policy to reflect changes in our practices or for legal reasons. Changes will be posted on this page with an updated effective date.`,
  },
];

const CookiePolicyPage = () => {
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
            <div className="w-14 h-14 mx-auto rounded-2xl bg-amber-500/10 flex items-center justify-center mb-4">
              <Cookie className="w-7 h-7 text-amber-400" />
            </div>
            <h1 className="text-4xl sm:text-5xl font-display font-bold text-white mb-4">
              Cookie Policy
            </h1>
            <p className="text-dark-400">Last updated: January 15, 2026</p>
          </div>

          <div className="card p-8 sm:p-10 space-y-8">
            <p className="text-dark-300 leading-relaxed">
              This Cookie Policy explains how FinPay uses cookies and similar tracking technologies
              when you visit our platform.
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
                Questions about cookies? Contact us at{' '}
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

export default CookiePolicyPage;
