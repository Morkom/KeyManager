import React, { useState, useEffect } from 'react';
import { Box, Button, TextField, Typography, Paper, Grid, Alert, FormControl, InputLabel, Select, MenuItem, Autocomplete } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const XsdToXmlGenerator: React.FC = () => {
  const [prepackagedXsdList, setPrepackagedXsdList] = useState<string[]>([]);
  const [selectedXsd, setSelectedXsd] = useState('');
  const [rootElements, setRootElements] = useState<string[]>([]);
  const [selectedRootElement, setSelectedRootElement] = useState('');
  const [generatedXml, setGeneratedXml] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPrepackaged = async () => {
      try {
        const res = await axios.get<string[]>(`${API_BASE_URL}/api/xml/prepackaged-xsds`);
        setPrepackagedXsdList(res.data);
      } catch (err) {
        console.error("Failed to fetch prepackaged XSDs");
      }
    };
    fetchPrepackaged();
  }, []);

  const handleXsdSelect = async (xsdName: string | null) => {
      setSelectedXsd(xsdName || '');
      setSelectedRootElement('');
      setGeneratedXml('');
      setError(null);
      setRootElements([]);

      if (!xsdName) {
          return;
      }

      try {
         const res = await axios.post(`${API_BASE_URL}/api/xml/generate`, { xsdFilename: xsdName });
         if (res.data.error) {
             setError(res.data.error);
         } else {
             setRootElements(res.data.rootElements || []);
         }
      } catch (err) {
          setError("Failed to fetch root elements from XSD.");
      }
  };

  const handleGenerate = async () => {
    setError(null);
    setGeneratedXml('');

    if (!selectedXsd || !selectedRootElement) {
        setError("Please select both an XSD and a Root Element.");
        return;
    }

    try {
      const res = await axios.post(`${API_BASE_URL}/api/xml/generate`, {
          xsdFilename: selectedXsd,
          rootElement: selectedRootElement
      });

      if (res.data.error) {
        setError(res.data.error);
      } else {
        setGeneratedXml(res.data.xml);
      }
    } catch (err) {
      setError("An error occurred while communicating with the server.");
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>XSD to XML Generator</Typography>
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
            <Autocomplete
              options={prepackagedXsdList}
              value={selectedXsd || null}
              onChange={(event, newValue) => handleXsdSelect(newValue)}
              renderInput={(params) => <TextField {...params} label="Select Pre-packaged XSD" />}
              sx={{ mb: 2 }}
            />

            <FormControl fullWidth disabled={!selectedXsd || rootElements.length === 0}>
                <InputLabel>Select Root Element</InputLabel>
                <Select value={selectedRootElement} label="Select Root Element" onChange={(e) => setSelectedRootElement(e.target.value)}>
                     {rootElements.map(name => <MenuItem key={name} value={name}>{name}</MenuItem>)}
                </Select>
            </FormControl>

            <Button variant="contained" sx={{ mt: 3 }} onClick={handleGenerate} disabled={!selectedXsd || !selectedRootElement}>
                Generate XML
            </Button>
            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        </Grid>

        <Grid item xs={12} md={6}>
          <Typography variant="h6">Generated XML</Typography>
          <TextField multiline rows={15} fullWidth value={generatedXml} InputProps={{ readOnly: true }} />
        </Grid>
      </Grid>
    </Paper>
  );
};

export default XsdToXmlGenerator;
