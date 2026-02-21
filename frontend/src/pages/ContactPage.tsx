import { Mail, MapPin, Phone, Send, MessageSquare, Clock } from 'lucide-react';
import { motion } from 'framer-motion';
import { useState } from 'react';

const contactMethods = [
  {
    icon: Mail,
    title: 'Email Us',
    description: 'Our team typically responds within 24 hours.',
    value: 'hello@finpay.com',
    href: 'mailto:hello@finpay.com',
  },
  {
    icon: Phone,
    title: 'Call Us',
    description: 'Mon-Fri from 8am to 6pm EST.',
    value: '+1 (555) 123-4567',
    href: 'tel:+15551234567',
  },
  {
    icon: MapPin,
    title: 'Visit Us',
    description: 'Come say hello at our headquarters.',
    value: '100 Finance St, New York, NY 10001',
    href: '#',
  },
  {
    icon: MessageSquare,
    title: 'Live Chat',
    description: 'Available 24/7 for Pro & Enterprise users.',
    value: 'Start a conversation',
    href: '#',
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.1, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const ContactPage = () => {
  const [submitted, setSubmitted] = useState(false);

  return (
    <div className="pt-24 pb-16">
      <section className="section-padding text-center">
        <motion.div
          className="max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-primary-500/10 text-primary-400 text-sm font-medium mb-4">
            Contact
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Get in <span className="text-gradient">Touch</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Have a question or want to learn more about FinPay? We'd love to hear
            from you. Reach out and our team will get back to you shortly.
          </p>
        </motion.div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16">
        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {contactMethods.map((method) => (
            <motion.a
              key={method.title}
              href={method.href}
              className="card card-hover p-6 group block"
              variants={itemVariants}
              whileHover={{ y: -5 }}
            >
              <div className="w-12 h-12 rounded-xl bg-primary-500/10 flex items-center justify-center mb-4 group-hover:bg-primary-500/20 transition-colors">
                <method.icon className="w-6 h-6 text-primary-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-1">{method.title}</h3>
              <p className="text-dark-400 text-sm mb-3">{method.description}</p>
              <span className="text-primary-400 text-sm font-medium">{method.value}</span>
            </motion.a>
          ))}
        </motion.div>
      </section>
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-16">
        <div className="grid lg:grid-cols-5 gap-12">
          <motion.div
            className="lg:col-span-3 card p-8"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            {submitted ? (
              <div className="text-center py-12">
                <div className="w-16 h-16 mx-auto rounded-full bg-secondary-500/10 flex items-center justify-center mb-4">
                  <Send className="w-8 h-8 text-secondary-400" />
                </div>
                <h3 className="text-2xl font-bold text-white mb-2">Message Sent!</h3>
                <p className="text-dark-400">
                  Thank you for reaching out. We'll get back to you within 24 hours.
                </p>
                <button
                  onClick={() => setSubmitted(false)}
                  className="btn-secondary mt-6"
                >
                  Send Another Message
                </button>
              </div>
            ) : (
              <>
                <h2 className="text-2xl font-bold text-white mb-6">Send Us a Message</h2>
                <form action={() => setSubmitted(true)} className="space-y-5">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-1.5">First Name</label>
                      <input
                        type="text"
                        required
                        className="w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500/50 transition-all"
                        placeholder="John"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-dark-300 mb-1.5">Last Name</label>
                      <input
                        type="text"
                        required
                        className="w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500/50 transition-all"
                        placeholder="Doe"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-dark-300 mb-1.5">Email</label>
                    <input
                      type="email"
                      required
                      className="w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500/50 transition-all"
                      placeholder="john@example.com"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-dark-300 mb-1.5">Subject</label>
                    <select
                      required
                      className="w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500/50 transition-all"
                    >
                      <option value="">Select a topic</option>
                      <option value="sales">Sales Inquiry</option>
                      <option value="support">Technical Support</option>
                      <option value="billing">Billing Question</option>
                      <option value="partnership">Partnership</option>
                      <option value="other">Other</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-dark-300 mb-1.5">Message</label>
                    <textarea
                      required
                      rows={4}
                      className="w-full px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500/50 transition-all resize-none"
                      placeholder="Tell us how we can help..."
                    />
                  </div>
                  <button type="submit" className="btn-primary w-full">
                    <Send className="w-4 h-4" />
                    Send Message
                  </button>
                </form>
              </>
            )}
          </motion.div>

          <motion.div
            className="lg:col-span-2 space-y-6"
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <div className="card p-6">
              <div className="flex items-center gap-3 mb-3">
                <Clock className="w-5 h-5 text-secondary-400" />
                <h4 className="font-semibold text-white">Business Hours</h4>
              </div>
              <ul className="space-y-2 text-sm text-dark-400">
                <li className="flex justify-between"><span>Monday – Friday</span><span className="text-dark-200">8:00 AM – 6:00 PM EST</span></li>
                <li className="flex justify-between"><span>Saturday</span><span className="text-dark-200">10:00 AM – 4:00 PM EST</span></li>
                <li className="flex justify-between"><span>Sunday</span><span className="text-dark-200">Closed</span></li>
              </ul>
            </div>
            <div className="card p-6">
              <h4 className="font-semibold text-white mb-3">Enterprise Inquiries</h4>
              <p className="text-dark-400 text-sm mb-4">
                Looking for a tailored solution for your organization? Our enterprise team
                is ready to help with custom integrations, SLAs, and dedicated support.
              </p>
              <a href="mailto:enterprise@finpay.com" className="text-primary-400 text-sm font-medium hover:text-primary-300 transition-colors">
                enterprise@finpay.com →
              </a>
            </div>
          </motion.div>
        </div>
      </section>
    </div>
  );
};

export default ContactPage;
