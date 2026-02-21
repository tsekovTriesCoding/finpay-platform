import { Calendar, ArrowRight, Tag } from 'lucide-react';
import { motion } from 'framer-motion';

const posts = [
  {
    title: 'Introducing FinPay 2.0: Instant Transfers Are Here',
    excerpt: 'We are thrilled to announce the biggest update in FinPay history. Instant transfers, revamped analytics, and a completely redesigned dashboard.',
    date: 'Feb 10, 2026',
    category: 'Product',
    readTime: '5 min read',
    featured: true,
  },
  {
    title: 'How We Built Our Real-Time Fraud Detection Engine',
    excerpt: 'A deep dive into the machine learning models and event-driven architecture behind our AI-powered fraud protection system.',
    date: 'Jan 28, 2026',
    category: 'Engineering',
    readTime: '8 min read',
    featured: false,
  },
  {
    title: 'FinPay Expands to 25 New Countries in APAC',
    excerpt: 'We are excited to bring FinPay\'s fast, secure payment solutions to millions more users across the Asia-Pacific region.',
    date: 'Jan 15, 2026',
    category: 'News',
    readTime: '3 min read',
    featured: false,
  },
  {
    title: 'Best Practices for Securing Your Digital Wallet',
    excerpt: 'Practical tips and features you should enable today to keep your FinPay account safe from threats.',
    date: 'Dec 20, 2025',
    category: 'Security',
    readTime: '6 min read',
    featured: false,
  },
  {
    title: 'Our Journey to SOC 2 Type II Certification',
    excerpt: 'The process, challenges, and lessons learned as we achieved one of the most rigorous security certifications in the industry.',
    date: 'Dec 5, 2025',
    category: 'Security',
    readTime: '7 min read',
    featured: false,
  },
  {
    title: 'Why We Chose a Microservices Architecture',
    excerpt: 'An inside look at how FinPay\'s engineering team designed the platform for scale, resilience, and developer velocity.',
    date: 'Nov 22, 2025',
    category: 'Engineering',
    readTime: '10 min read',
    featured: false,
  },
];

const categoryColors: Record<string, string> = {
  Product: 'bg-primary-400/10 text-primary-400',
  Engineering: 'bg-violet-400/10 text-violet-400',
  News: 'bg-secondary-400/10 text-secondary-400',
  Security: 'bg-amber-400/10 text-amber-400',
};

const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.1, delayChildren: 0.2 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const BlogPage = () => {
  const featured = posts.find((p) => p.featured);
  const rest = posts.filter((p) => !p.featured);

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
            Blog
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-display font-bold text-white mb-6">
            Insights & <span className="text-gradient">Updates</span>
          </h1>
          <p className="text-lg text-dark-300 max-w-2xl mx-auto">
            Product announcements, engineering deep dives, and industry perspectives
            from the FinPay team.
          </p>
        </motion.div>
      </section>

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-16">
        {featured && (
          <motion.div
            className="card card-hover p-8 mb-12 cursor-pointer group"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            whileHover={{ y: -5 }}
          >
            <div className="flex items-center gap-3 mb-4">
              <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${categoryColors[featured.category]}`}>
                {featured.category}
              </span>
              <span className="text-xs text-dark-500">Featured</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-white mb-3 group-hover:text-gradient transition-all duration-300">
              {featured.title}
            </h2>
            <p className="text-dark-300 mb-5 max-w-3xl">{featured.excerpt}</p>
            <div className="flex items-center gap-4 text-sm text-dark-400">
              <span className="flex items-center gap-1.5">
                <Calendar className="w-4 h-4" />
                {featured.date}
              </span>
              <span>{featured.readTime}</span>
            </div>
          </motion.div>
        )}

        <motion.div
          className="grid grid-cols-1 md:grid-cols-2 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {rest.map((post) => (
            <motion.div
              key={post.title}
              className="card card-hover p-6 cursor-pointer group"
              variants={itemVariants}
              whileHover={{ y: -5 }}
            >
              <div className="flex items-center gap-2 mb-3">
                <Tag className="w-3.5 h-3.5 text-dark-500" />
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${categoryColors[post.category]}`}>
                  {post.category}
                </span>
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-gradient transition-all duration-300">
                {post.title}
              </h3>
              <p className="text-dark-400 text-sm mb-4 leading-relaxed">{post.excerpt}</p>
              <div className="flex items-center justify-between text-xs text-dark-500">
                <span className="flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5" />
                  {post.date}
                </span>
                <span>{post.readTime}</span>
              </div>
            </motion.div>
          ))}
        </motion.div>

        <motion.div
          className="card p-8 mt-12 text-center"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <h3 className="text-xl font-bold text-white mb-2">Stay in the Loop</h3>
          <p className="text-dark-400 text-sm mb-5 max-w-md mx-auto">
            Subscribe to the FinPay newsletter for the latest product updates and insights.
          </p>
          <form
            onSubmit={(e) => e.preventDefault()}
            className="flex flex-col sm:flex-row gap-3 max-w-md mx-auto"
          >
            <input
              type="email"
              placeholder="you@example.com"
              className="flex-1 px-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 transition-all"
            />
            <button type="submit" className="btn-primary shrink-0">
              Subscribe
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>
        </motion.div>
      </div>
    </div>
  );
};

export default BlogPage;
