import React, { useState, useMemo, useEffect } from 'react';
import { CssBaseline, GlobalStyles, Paper } from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { AuthProvider } from './AuthContext';
import ProtectedRoute from './ProtectedRoute';
import Layout from './Layout';
import LoginPage from './LoginPage';
import CaManagement from './CaManagement';
import CreateCsrForm from './CreateCsrForm';
import SignCertificateForm from './SignCertificateForm';
import RevocationManagement from './RevocationManagement';
import KeystoreManager from './KeystoreManager';
import AuditLog from './AuditLog';
import UserManagement from './UserManagement';
import SshKeyGenerator from './SshKeyGenerator';

const AppContent: React.FC = () => {
  const { i18n } = useTranslation();
  const [mode, setMode] = useState<'light' | 'dark'>(() => {
    const savedMode = localStorage.getItem('themeMode') as 'light' | 'dark';
    return savedMode || 'light';
  });

  useEffect(() => {
    localStorage.setItem('themeMode', mode);
  }, [mode]);

  useEffect(() => {
    const savedLang = localStorage.getItem('language');
    if (savedLang && savedLang !== i18n.language) {
      i18n.changeLanguage(savedLang);
    }
  }, [i18n]);

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          ...(mode === 'light'
            ? { background: { default: 'linear-gradient(to right, #f5f7fa, #e3e8f0)' } }
            : { background: { default: '#121212' } }),
        },
      }),
    [mode],
  );

  const toggleColorMode = () => {
    setMode((prevMode) => (prevMode === 'light' ? 'dark' : 'light'));
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <GlobalStyles styles={{ body: { background: theme.palette.background.default } }} />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Layout mode={mode} toggleColorMode={toggleColorMode} />}>
            <Route index element={<Navigate to="/ssh" replace />} />
            <Route path="ca" element={<CaManagement />} />
            <Route path="csr" element={<CreateCsrForm />} />
            <Route path="sign" element={<SignCertificateForm />} />
            <Route path="revoke" element={<RevocationManagement />} />
            <Route path="keystore" element={<KeystoreManager />} />
            <Route path="audit" element={<AuditLog />} />
            <Route path="users" element={<UserManagement />} />
            <Route path="ssh" element={<SshKeyGenerator />} />
          </Route>
        </Route>
      </Routes>
    </ThemeProvider>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  );
}
