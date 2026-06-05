import React, { useState, useEffect, useRef } from 'react';
import { Box, Button, TextField, Typography, Paper, Grid, Alert, Switch, FormControlLabel, Select, MenuItem, InputLabel, FormControl, Autocomplete } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

type InputMode = 'text' | 'file' | 'prepackaged';

const XmlValidator: React.FC = () => {
  const [xmlInputMode, setXmlInputMode] = useState<InputMode>('text');
  const [xsdInputMode, setXsdInputMode] = useState<InputMode>('text');
  const [xmlText, setXmlText] = useState('<note>\n  <to>Tove</to>\n  <from>Jani</from>\n  <heading>Reminder</heading>\n  <body>Don\'t forget me this weekend!</body>\n</note>');
  const [xsdText, setXsdText] = useState('');
  const [xmlFile, setXmlFile] = useState<File | null>(null);
  const [xsdFile, setXsdFile] = useState<File | null>(null);
  const [prepackagedXsd, setPrepackagedXsd] = useState<string | null>(null);
  const [prepackagedXsdList, setPrepackagedXsdList] = useState<string[]>([]);
  const [result, setResult] = useState<{ valid: boolean; message: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const xmlFileRef = useRef<HTMLInputElement>(null);
  const xsdFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const fetchPrepackaged = async () => {
      try {
        const res = await axios.get<string[]>(`${API_BASE_URL}/api/xml/prepackaged-xsds`);
        setPrepackagedXsdList(res.data);
        if (res.data.length > 0) {
          setPrepackagedXsd(res.data[0]);
        }
      } catch (err) {
        console.error("Failed to fetch prepackaged XSDs");
      }
    };
    fetchPrepackaged();
  }, []);

  const handleSubmit = async () => {
    setError(null);
    setResult(null);

    const getFileFromText = (text: string, name: string, type: string) => new File([text], name, { type });

    const finalXmlFile = xmlInputMode === 'file' ? xmlFile : getFileFromText(xmlText, 'xml-input.xml', 'application/xml');
    if (!finalXmlFile) {
      setError("XML source is required.");
      return;
    }

    const formData = new FormData();
    formData.append('xmlFile', finalXmlFile);

    if (xsdInputMode === 'text') {
      formData.append('xsdFile', getFileFromText(xsdText, 'xsd-input.xsd', 'application/xml'));
    } else if (xsdInputMode === 'file' && xsdFile) {
      formData.append('xsdFile', xsdFile);
    } else if (xsdInputMode === 'prepackaged' && prepackagedXsd) {
      formData.append('prepackagedXsd', prepackagedXsd);
    } else {
      setError("XSD source is required.");
      return;
    }

    try {
      const res = await axios.post(`${API_BASE_URL}/api/xml/validate`, formData);
      setResult(res.data);
    } catch (err) {
      setError("An error occurred while communicating with the server.");
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>XML Validator</Typography>
      <Grid container spacing={3}>
        {/* XML Input */}
        <Grid item xs={12} md={6}>
          <Typography variant="h6">XML Source</Typography>
          <FormControlLabel control={<Switch checked={xmlInputMode === 'file'} onChange={(e) => setXmlInputMode(e.target.checked ? 'file' : 'text')} />} label="Use File Upload" />
          {xmlInputMode === 'text' ? (
            <TextField label="XML Content" multiline rows={15} fullWidth value={xmlText} onChange={(e) => setXmlText(e.target.value)} />
          ) : (
            <Button variant="outlined" fullWidth onClick={() => xmlFileRef.current?.click()}>{xmlFile ? `Selected: ${xmlFile.name}` : "Select XML File"}</Button>
          )}
          <input type="file" ref={xmlFileRef} hidden accept=".xml" onChange={(e) => setXmlFile(e.target.files?.[0] || null)} />
        </Grid>

        {/* XSD Input */}
        <Grid item xs={12} md={6}>
          <Typography variant="h6">XSD Schema</Typography>
          <FormControl fullWidth size="small" sx={{ mb: 1 }}>
            <InputLabel>Schema Source</InputLabel>
            <Select value={xsdInputMode} label="Schema Source" onChange={(e) => setXsdInputMode(e.target.value as InputMode)}>
              <MenuItem value="text">Text Input</MenuItem>
              <MenuItem value="file">File Upload</MenuItem>
              <MenuItem value="prepackaged">Pre-packaged</MenuItem>
            </Select>
          </FormControl>
          {xsdInputMode === 'text' && <TextField label="XSD Content" multiline rows={13} fullWidth value={xsdText} onChange={(e) => setXsdText(e.target.value)} />}
          {xsdInputMode === 'file' && <Button variant="outlined" fullWidth onClick={() => xsdFileRef.current?.click()}>{xsdFile ? `Selected: ${xsdFile.name}` : "Select XSD File"}</Button>}
          {xsdInputMode === 'prepackaged' && (
            <Autocomplete
              options={prepackagedXsdList}
              value={prepackagedXsd}
              onChange={(event, newValue) => setPrepackagedXsd(newValue)}
              renderInput={(params) => <TextField {...params} label="Select Schema" />}
            />
          )}
          <input type="file" ref={xsdFileRef} hidden accept=".xsd" onChange={(e) => setXsdFile(e.target.files?.[0] || null)} />
        </Grid>
      </Grid>

      <Button variant="contained" sx={{ mt: 3 }} onClick={handleSubmit}>Validate</Button>

      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {result && (
        <Alert severity={result.valid ? 'success' : 'error'} sx={{ mt: 2, whiteSpace: 'pre-wrap' }}>
          {result.message}
        </Alert>
      )}
    </Paper>
  );
};

export default XmlValidator;
