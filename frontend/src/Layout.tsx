import React from 'react';
import { Box, Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography, Select, MenuItem, FormControl, SelectChangeEvent, IconButton, Paper, Divider } from '@mui/material';
import { Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import NoteAddIcon from '@mui/icons-material/NoteAdd';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import GppBadIcon from '@mui/icons-material/GppBad';
import KeyIcon from '@mui/icons-material/Key';
import PolicyIcon from '@mui/icons-material/Policy';
import LogoutIcon from '@mui/icons-material/Logout';
import SupervisorAccountIcon from '@mui/icons-material/SupervisorAccount';
import VpnKeyIcon from '@mui/icons-material/VpnKey'; // Corrected Icon
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';

const drawerWidth = 240;

interface LayoutProps {
  mode: 'light' | 'dark';
  toggleColorMode: () => void;
}

const languages = [
  { code: 'en', name: 'English', flag: '🇬🇧' },
  { code: 'cs', name: 'Čeština', flag: '🇨🇿' },
];

const Layout: React.FC<LayoutProps> = ({ mode, toggleColorMode }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const auth = useAuth();

  const handleLanguageChange = (event: SelectChangeEvent<string>) => {
    const newLang = event.target.value;
    i18n.changeLanguage(newLang);
    localStorage.setItem('language', newLang);
  };

  const menuItems = [
    { text: "SSH Keys", icon: <VpnKeyIcon />, path: '/ssh' },
    { text: t('keystoreManager'), icon: <KeyIcon />, path: '/keystore' },
    { text: t('tabCa'), icon: <AccountBalanceIcon />, path: '/ca' },
    { text: t('tabCsr'), icon: <NoteAddIcon />, path: '/csr' },
    { text: t('tabSign'), icon: <VerifiedUserIcon />, path: '/sign' },
    { text: t('tabRevoke'), icon: <GppBadIcon />, path: '/revoke' },
    { text: "Audit Log", icon: <PolicyIcon />, path: '/audit' },
  ];

  const adminMenuItems = [
    { text: "User Management", icon: <SupervisorAccountIcon />, path: '/users' },
  ];

  return (
    <Box sx={{ display: 'flex' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          <List>
            {menuItems.map((item) => (
              <ListItem key={item.text} disablePadding>
                <ListItemButton onClick={() => navigate(item.path)}>
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.text} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
          <Divider />
          {auth.roles.includes('ROLE_ADMIN') && (
            <List>
              {adminMenuItems.map((item) => (
                <ListItem key={item.text} disablePadding>
                  <ListItemButton onClick={() => navigate(item.path)}>
                    <ListItemIcon>{item.icon}</ListItemIcon>
                    <ListItemText primary={item.text} />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          )}
          <Divider />
          <List>
            <ListItem disablePadding>
              <ListItemButton onClick={auth.logout}>
                <ListItemIcon><LogoutIcon /></ListItemIcon>
                <ListItemText primary="Logout" />
              </ListItemButton>
            </ListItem>
          </List>
        </Box>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3, height: '100vh', overflow: 'auto' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h4" component="h1" color="text.primary">
            {t('appTitle')}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Logged in as: {auth.username}
            </Typography>
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
              {mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
            </IconButton>
          </Box>
        </Box>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Outlet />
        </Paper>
      </Box>
    </Box>
  );
};

export default Layout;
