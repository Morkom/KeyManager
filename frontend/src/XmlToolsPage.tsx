import React, { useState } from 'react';
import { Box, Tabs, Tab, Paper } from '@mui/material';
import XmlValidator from './XmlValidator';
import XsltTransformer from './XsltTransformer';
import XsdToXmlGenerator from './XsdToXmlGenerator';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} id={`xml-tabpanel-${index}`} aria-labelledby={`xml-tab-${index}`} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

const XmlToolsPage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Box>
      <Paper>
        <Tabs value={tabValue} onChange={handleTabChange} centered>
          <Tab label="XML Validator" />
          <Tab label="XSLT Transformer" />
          <Tab label="XSD to XML Generator" />
        </Tabs>
      </Paper>
      <TabPanel value={tabValue} index={0}>
        <XmlValidator />
      </TabPanel>
      <TabPanel value={tabValue} index={1}>
        <XsltTransformer />
      </TabPanel>
      <TabPanel value={tabValue} index={2}>
        <XsdToXmlGenerator />
      </TabPanel>
    </Box>
  );
};

export default XmlToolsPage;
