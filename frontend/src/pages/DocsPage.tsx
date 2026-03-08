import { Code2, ExternalLink } from 'lucide-react';
import { motion } from 'framer-motion';
import { useState } from 'react';

const GATEWAY_URL = import.meta.env.VITE_API_URL?.replace(/\/api$/, '') || 'http://localhost:8080';

const services = [
  { name: 'Auth Service', key: 'auth-service' },
  { name: 'User Service', key: 'user-service' },
  { name: 'Payment Service', key: 'payment-service' },
  { name: 'Wallet Service', key: 'wallet-service' },
  { name: 'Notification Service', key: 'notification-service' },
];

const DocsPage = () => {
  const [activeService, setActiveService] = useState(services[0].key);
  const swaggerUrl = `${GATEWAY_URL}/swagger-ui/index.html?urls.primaryName=${encodeURIComponent(
    services.find((s) => s.key === activeService)?.name ?? services[0].name
  )}`;

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
            Explore our live API documentation powered by SpringDoc OpenAPI.
            Select a service below to view its endpoints, schemas, and try requests.
          </p>
        </motion.div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-8">
        <div className="flex flex-wrap items-center justify-center gap-2 mb-6">
          {services.map((service) => (
            <button
              key={service.key}
              onClick={() => setActiveService(service.key)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                activeService === service.key
                  ? 'bg-primary-500 text-white'
                  : 'bg-dark-800/50 text-dark-300 hover:bg-dark-700/50 hover:text-white'
              }`}
            >
              {service.name}
            </button>
          ))}
        </div>

        <div className="flex justify-end mb-2">
          <a
            href={swaggerUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 text-sm text-dark-400 hover:text-primary-400 transition-colors"
          >
            Open in new tab
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>

        <motion.div
          className="card overflow-hidden rounded-xl border border-dark-700/50"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
        >
          <iframe
            key={activeService}
            src={swaggerUrl}
            title={`${activeService} API Documentation`}
            className="w-full border-0"
            style={{ height: 'calc(100vh - 20rem)', minHeight: '600px' }}
          />
        </motion.div>
      </section>
    </div>
  );
};

export default DocsPage;
