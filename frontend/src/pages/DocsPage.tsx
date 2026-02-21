import { Code2, Key, BookOpen, Terminal, ArrowRight, Copy } from 'lucide-react';
import { motion } from 'framer-motion';
import { useState } from 'react';

const endpoints = [
  { method: 'POST', path: '/api/v1/payments', description: 'Create a new payment' },
  { method: 'GET', path: '/api/v1/payments/:id', description: 'Retrieve payment details' },
  { method: 'GET', path: '/api/v1/wallets', description: 'List user wallets' },
  { method: 'POST', path: '/api/v1/wallets/transfer', description: 'Transfer between wallets' },
  { method: 'GET', path: '/api/v1/users/me', description: 'Get current user profile' },
  { method: 'GET', path: '/api/v1/transactions', description: 'List transactions with filters' },
];

const methodColors: Record<string, string> = {
  GET: 'text-green-400 bg-green-400/10',
  POST: 'text-blue-400 bg-blue-400/10',
  PUT: 'text-amber-400 bg-amber-400/10',
  DELETE: 'text-red-400 bg-red-400/10',
};

const codeExample = `curl -X POST https://api.finpay.com/v1/payments \\
  -H "Authorization: Bearer sk_live_..." \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 5000,
    "currency": "usd",
    "recipient": "user_abc123",
    "description": "Invoice #1234"
  }'`;

const sdks = [
  { name: 'JavaScript / Node.js', status: 'Stable' },
  { name: 'Python', status: 'Stable' },
  { name: 'Java', status: 'Stable' },
  { name: 'Go', status: 'Beta' },
  { name: 'Ruby', status: 'Coming Soon' },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.08, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
};

const DocsPage = () => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(codeExample);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

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
            <Code2 className="w-4 h-4 inline mr-1 -mt-0.5" />
            API Documentation
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Build with the FinPay{' '}
            <span className="text-gradient">API</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Integrate payments, wallets, and financial services into your application
            with our well-documented RESTful API.
          </p>
        </motion.div>
      </section>

      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <div className="grid lg:grid-cols-2 gap-8">
          <motion.div
            className="card overflow-hidden"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <div className="flex items-center justify-between px-4 py-3 bg-dark-800/50 border-b border-dark-700/50">
              <div className="flex items-center gap-2 text-sm text-dark-400">
                <Terminal className="w-4 h-4" />
                Quick Start
              </div>
              <button
                onClick={handleCopy}
                className="flex items-center gap-1.5 text-xs text-dark-400 hover:text-white transition-colors"
              >
                <Copy className="w-3.5 h-3.5" />
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
            <pre className="p-5 text-sm text-dark-200 overflow-x-auto leading-relaxed">
              <code>{codeExample}</code>
            </pre>
          </motion.div>


          <motion.div
            className="space-y-4"
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            {[
              { icon: Key, title: 'Authentication', desc: 'Use API keys to authenticate requests. Manage keys from your dashboard.' },
              { icon: BookOpen, title: 'Guides & Tutorials', desc: 'Step-by-step guides to get you up and running quickly.' },
              { icon: Code2, title: 'SDKs & Libraries', desc: 'Official client libraries for popular programming languages.' },
            ].map((item, i) => (
              <motion.div
                key={item.title}
                className="card card-hover p-5 flex items-start gap-4 cursor-pointer"
                whileHover={{ y: -3 }}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 + i * 0.1 }}
              >
                <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center shrink-0">
                  <item.icon className="w-5 h-5 text-primary-400" />
                </div>
                <div>
                  <h3 className="font-semibold text-white mb-1">{item.title}</h3>
                  <p className="text-sm text-dark-400">{item.desc}</p>
                </div>
                <ArrowRight className="w-4 h-4 text-dark-500 shrink-0 mt-1" />
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.h2
          className="text-2xl font-display font-bold text-white mb-8"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Core Endpoints
        </motion.h2>
        <motion.div
          className="card overflow-hidden divide-y divide-dark-800/50"
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
        >
          {endpoints.map((ep) => (
            <motion.div
              key={ep.path}
              className="flex items-center gap-4 px-5 py-4 hover:bg-dark-800/30 transition-colors cursor-pointer"
              variants={itemVariants}
            >
              <span className={`text-xs font-mono font-bold px-2.5 py-1 rounded-md ${methodColors[ep.method]}`}>
                {ep.method}
              </span>
              <code className="text-sm text-dark-200 font-mono flex-1">{ep.path}</code>
              <span className="text-sm text-dark-400 hidden sm:block">{ep.description}</span>
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-16">
        <motion.h2
          className="text-2xl font-display font-bold text-white mb-8"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          Official SDKs
        </motion.h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {sdks.map((sdk, i) => (
            <motion.div
              key={sdk.name}
              className="card card-hover p-5 flex items-center justify-between"
              initial={{ opacity: 0, y: 15 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08 }}
              whileHover={{ y: -3 }}
            >
              <span className="text-white font-medium">{sdk.name}</span>
              <span className={`text-xs px-2.5 py-1 rounded-full ${
                sdk.status === 'Stable'
                  ? 'bg-green-400/10 text-green-400'
                  : sdk.status === 'Beta'
                    ? 'bg-amber-400/10 text-amber-400'
                    : 'bg-dark-700 text-dark-400'
              }`}>
                {sdk.status}
              </span>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default DocsPage;
