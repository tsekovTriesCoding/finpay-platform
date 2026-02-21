import { Outlet, ScrollRestoration, useNavigation } from 'react-router-dom';

import Navbar from './Navbar';
import Footer from './Footer';

const Layout = () => {
  const { state } = useNavigation();

  return (
    <div className="min-h-screen flex flex-col">
      {state === 'loading' && (
        <div className="fixed inset-x-0 top-0 z-50 h-0.5">
          <div className="h-full w-1/3 bg-primary-500 animate-[progress_1s_ease-in-out_infinite]" />
        </div>
      )}
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      <ScrollRestoration />
    </div>
  );
};

export default Layout;
