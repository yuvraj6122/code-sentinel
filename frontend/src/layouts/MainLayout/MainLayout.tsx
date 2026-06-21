import type { ReactNode } from 'react';
import { Footer } from '../../components/Footer/Footer';
import { Header } from '../../components/Header/Header';
import styles from './MainLayout.module.css';

interface MainLayoutProps {
  children: ReactNode;
}

export function MainLayout({ children }: MainLayoutProps) {
  return (
    <div className={styles.layout}>
      <Header />
      <main className={styles.main}>{children}</main>
      <Footer />
    </div>
  );
}
