import React, { useState, useMemo, useEffect } from 'react';
import { Container, Typography, CssBaseline, Box, Tabs, Tab, Select, MenuItem, FormControl, SelectChangeEvent, GlobalStyles, Paper, IconButton } from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import CaManagement from './CaManagement';
import CreateCsrForm from './CreateCsrForm';
import SignCertificateForm from './SignCertificateForm';
import RevocationManagement from './RevocationManagement';
import KeystoreManager from './KeystoreManager';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import NoteAddIcon from '@mui/icons-material/NoteAdd';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import GppBadIcon from '@mui/icons-material/GppBad';
import KeyIcon from '@mui/icons-material/Key';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} id={`simple-tabpanel-${index}`} aria-labelledby={`simple-tab-${index}`} {...other}>
      {value === index && <Box>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return { id: `simple-tab-${index}`, 'aria-controls': `simple-tabpanel-${index}` };
}

const languages = [
  { code: 'en', name: 'English', flag: '🇬🇧' },
  { code: 'cs', name: 'Čeština', flag: '🇨🇿' },
];

const AppContent: React.FC = () => {
  const { t, i18n } = useTranslation();
  const [tabValue, setTabValue] = useState(0);

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
            ? { background: { default: 'linear-gradient(to right, #f5f7fa, #e3e8f0)', paper: '#ffffff' } }
            : { background: { default: '#121212', paper: '#1e1e1e' } }),
        },
      }),
    [mode],
  );

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleLanguageChange = (event: SelectChangeEvent<string>) => {
    const newLang = event.target.value;
    i18n.changeLanguage(newLang);
    localStorage.setItem('language', newLang);
  };

  const toggleColorMode = () => {
    setMode((prevMode) => (prevMode === 'light' ? 'dark' : 'light'));
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <GlobalStyles styles={{ body: { background: theme.palette.background.default } }} />
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h4" component="h1" color="text.primary">
              {t('appTitle')}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <FormControl size="small" variant="outlined">
                <Select
                  value={i18n.language}
                  onChange={handleLanguageChange}
                  renderValue={(value) => {
                    const lang = languages.find(l => l.code === value);
                    return `${lang?.flag} ${lang?.name}`;
                  }}
                >
                  {languages.map((lang) => (
                    <MenuItem key={lang.code} value={lang.code}>
                      {lang.flag} {lang.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <IconButton onClick={toggleColorMode} color="inherit">
                {theme.palette.mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
              </IconButton>
            </Box>
          </Box>

          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={tabValue} onChange={handleTabChange} aria-label="main sections" variant="fullWidth" textColor="inherit">
              <Tab icon={<AccountBalanceIcon />} iconPosition="start" label={t('tabCa')} {...a11yProps(0)} />
              <Tab icon={<NoteAddIcon />} iconPosition="start" label={t('tabCsr')} {...a11yProps(1)} />
              <Tab icon={<VerifiedUserIcon />} iconPosition="start" label={t('tabSign')} {...a11yProps(2)} />
              <Tab icon={<GppBadIcon />} iconPosition="start" label={t('tabRevoke')} {...a11yProps(3)} />
              <Tab icon={<KeyIcon />} iconPosition="start" label="Keystore Manager" {...a11yProps(4)} />
            </Tabs>
          </Box>

          <Paper elevation={2} sx={{ borderTopLeftRadius: 0, borderTopRightRadius: 0 }}>
            <TabPanel value={tabValue} index={0}>
              <Box sx={{ p: 3 }}><CaManagement /></Box>
            </TabPanel>
            <TabPanel value={tabValue} index={1}>
              <Box sx={{ p: 3 }}><CreateCsrForm /></Box>
            </TabPanel>
            <TabPanel value={tabValue} index={2}>
              <Box sx={{ p: 3 }}><SignCertificateForm /></Box>
            </TabPanel>
            <TabPanel value={tabValue} index={3}>
              <Box sx={{ p: 3 }}><RevocationManagement /></Box>
            </TabPanel>
            <TabPanel value={tabValue} index={4}>
              <Box sx={{ p: 3 }}><KeystoreManager /></Box>
            </TabPanel>
          </Paper>
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default function App() {
  return <AppContent />;
}
