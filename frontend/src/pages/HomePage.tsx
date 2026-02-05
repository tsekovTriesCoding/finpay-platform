import HeroSection from '../components/home/HeroSection';
import FeaturesSection from '../components/home/FeaturesSection';
import StatsSection from '../components/home/StatsSection';
import SecuritySection from '../components/home/SecuritySection';
import CTASection from '../components/home/CTASection';

const HomePage = () => {
  return (
    <>
      <HeroSection />
      <StatsSection />
      <FeaturesSection />
      <SecuritySection />
      <CTASection />
    </>
  );
};

export default HomePage;
