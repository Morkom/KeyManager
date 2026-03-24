import React, { useState } from 'react';
import { Box, Tabs, Tab, Paper } from '@mui/material';
import EncodingTool from './EncodingTool';
import EncryptionTool from './EncryptionTool';
import BcryptTool from './BcryptTool';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} id={`crypto-tabpanel-${index}`} aria-labelledby={`crypto-tab-${index}`} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const CryptoToolsPage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Box>
      <Paper>
        <Tabs value={tabValue} onChange={handleTabChange} centered>
          <Tab label="Encoding / Decoding" />
          <Tab label="Encryption / Decryption" />
          <Tab label="Bcrypt" />
        </Tabs>
      </Paper>
      <TabPanel value={tabValue} index={0}>
        <EncodingTool />
      </TabPanel>
      <TabPanel value={tabValue} index={1}>
        <EncryptionTool />
      </TabPanel>
      <TabPanel value={tabValue} index={2}>
        <BcryptTool />
      </TabPanel>
    </Box>
  );
};

export default CryptoToolsPage;
