import { MainLayout } from './layouts/MainLayout/MainLayout';
import { DashboardPage } from './pages/DashboardPage/DashboardPage';

export function App() {
  return (
    <MainLayout>
      <DashboardPage />
    </MainLayout>
  );
}
