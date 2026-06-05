import React, { useState, useRef, useEffect } from 'react';
import { Box, Button, TextField, Typography, Paper, Grid, Alert, Switch, FormControlLabel, Select, MenuItem, InputLabel, FormControl } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

type InputMode = 'text' | 'file';
type KeystoreType = 'JKS' | 'PKCS12';

const XmlSigner: React.FC = () => {
  const [xmlInputMode, setXmlInputMode] = useState<InputMode>('text');
  const [xmlText, setXmlText] = useState('<note>\n  <to>Tove</to>\n  <from>Jani</from>\n  <heading>Reminder</heading>\n  <body>Don\'t forget me this weekend!</body>\n</note>');
  const [xmlFile, setXmlFile] = useState<File | null>(null);
  const [keystoreFile, setKeystoreFile] = useState<File | null>(null);
  const [keystoreType, setKeystoreType] = useState<KeystoreType>('JKS');
  const [aliases, setAliases] = useState<string[]>([]);
  const [selectedAlias, setSelectedAlias] = useState('');
  const [storePassword, setStorePassword] = useState('changeit');
  const [keyPassword, setKeyPassword] = useState('changeit');
  const [signedXml, setSignedXml] = useState('');
  const [error, setError] = useState<string | null>(null);

  const xmlFileRef = useRef<HTMLInputElement>(null);
  const keystoreFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const fetchAliases = async () => {
      if (!keystoreFile || !storePassword) {
        setAliases([]);
        setSelectedAlias('');
        return;
      }

      const formData = new FormData();
      formData.append('keystoreFile', keystoreFile);
      formData.append('storeType', keystoreType);
      formData.append('storePassword', storePassword);

      try {
        const res = await axios.post<string[]>(`${API_BASE_URL}/api/xml/keystore-aliases`, formData);
        setAliases(res.data);
        if (res.data.length > 0) {
          setSelectedAlias(res.data[0]);
        } else {
          setSelectedAlias('');
        }
        setError(null);
      } catch (err: any) {
        setAliases([]);
        setSelectedAlias('');
        if (err.response) {
            setError(err.response.data || "Error fetching aliases from keystore.");
        } else {
            setError("Error fetching aliases from keystore.");
        }
      }
    };

    const debounce = setTimeout(fetchAliases, 500);
    return () => clearTimeout(debounce);
  }, [keystoreFile, storePassword, keystoreType]);

  const handleSubmit = async () => {
    setError(null);
    setSignedXml('');

    const getFileFromText = (text: string, name: string, type: string) => new File([text], name, { type });

    const finalXmlFile = xmlInputMode === 'file' ? xmlFile : getFileFromText(xmlText, 'xml-input.xml', 'application/xml');
    if (!finalXmlFile) {
      setError("XML source is required.");
      return;
    }

    if (!keystoreFile) {
      setError("Keystore file is required.");
      return;
    }

    if (!selectedAlias || !storePassword || !keyPassword) {
        setError("Alias, Store Password, and Key Password are required.");
        return;
    }

    const formData = new FormData();
    formData.append('xmlFile', finalXmlFile);
    formData.append('keystoreFile', keystoreFile);
    formData.append('storeType', keystoreType);
    formData.append('alias', selectedAlias);
    formData.append('storePassword', storePassword);
    formData.append('keyPassword', keyPassword);

    try {
      const res = await axios.post(`${API_BASE_URL}/api/xml/sign`, formData, {
          headers: {
              'Content-Type': 'multipart/form-data'
          }
      });
      setSignedXml(res.data);
    } catch (err: any) {
        if (err.response) {
            setError(err.response.data);
        } else {
            setError("An error occurred while communicating with the server.");
        }
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>XML Signer</Typography>
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

        {/* Keystore and Signature Details */}
        <Grid item xs={12} md={6}>
            <Typography variant="h6">Signature Details</Typography>
            <FormControl fullWidth sx={{ mt: 2 }}>
                <InputLabel>Keystore Type</InputLabel>
                <Select value={keystoreType} label="Keystore Type" onChange={(e) => setKeystoreType(e.target.value as KeystoreType)}>
                    <MenuItem value="JKS">JKS</MenuItem>
                    <MenuItem value="PKCS12">PKCS12</MenuItem>
                </Select>
            </FormControl>
            <Button variant="outlined" fullWidth onClick={() => keystoreFileRef.current?.click()} sx={{ mt: 2 }}>{keystoreFile ? `Selected: ${keystoreFile.name}` : "Select Keystore File"}</Button>
            <input type="file" ref={keystoreFileRef} hidden accept=".jks,.p12" onChange={(e) => setKeystoreFile(e.target.files?.[0] || null)} />

            <TextField label="Store Password" type="password" fullWidth value={storePassword} onChange={(e) => setStorePassword(e.target.value)} sx={{ mt: 2 }} />

            <FormControl fullWidth sx={{ mt: 2 }} disabled={aliases.length === 0}>
              <InputLabel>Alias</InputLabel>
              <Select value={selectedAlias} label="Alias" onChange={(e) => setSelectedAlias(e.target.value)}>
                {aliases.map(name => <MenuItem key={name} value={name}>{name}</MenuItem>)}
              </Select>
            </FormControl>

            <TextField label="Key Password" type="password" fullWidth value={keyPassword} onChange={(e) => setKeyPassword(e.target.value)} sx={{ mt: 2 }} />
        </Grid>
      </Grid>

      <Button variant="contained" sx={{ mt: 3 }} onClick={handleSubmit}>Sign XML</Button>

      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

      {signedXml && (
        <Box sx={{ mt: 3 }}>
            <Typography variant="h6">Signed XML</Typography>
            <TextField multiline rows={15} fullWidth value={signedXml} InputProps={{ readOnly: true }} />
        </Box>
      )}
    </Paper>
  );
};

export default XmlSigner;
