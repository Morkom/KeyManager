import React, { useState, useEffect } from 'react';
import {
  Box, Button, TextField, Typography, Paper, Grid, Alert,
  FormControl, InputLabel, Select, MenuItem, Checkbox, ListItemText, OutlinedInput
} from '@mui/material';
import { ContentCopy } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface ExtensionDto {
    id: string;
    name: string;
    description: string;
}

interface AlgorithmDto {
    id: string;
    name: string;
    description: string;
}

const CreateCsrForm: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    commonName: 'example.com',
    organization: 'My Example Corp',
    organizationalUnit: 'IT Department',
    locality: 'Brno',
    state: 'CZ',
    country: 'CZ',
    keyAlgorithm: 'RSA-4096',
    password: '',
    pemAlgorithm: 'AES_256_CBC',
    extensions: [] as string[],
  });
  const [response, setResponse] = useState<{ csrPem: string; privateKeyDownloadUrl: string; privateKeyCacheId: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copySuccess, setCopySuccess] = useState(false);
  const [availableExtensions, setAvailableExtensions] = useState<ExtensionDto[]>([]);
  const [availableAlgorithms, setAvailableAlgorithms] = useState<AlgorithmDto[]>([]);

  useEffect(() => {
    const fetchExtensions = async () => {
        try {
            const res = await axios.get(`${API_BASE_URL}/api/extensions`);
            setAvailableExtensions(res.data);
        } catch (err) {
            console.error("Failed to fetch extensions:", err);
        }
    };
    const fetchAlgorithms = async () => {
        try {
            const res = await axios.get(`${API_BASE_URL}/api/algorithms`);
            setAvailableAlgorithms(res.data);
        } catch (err) {
            console.error("Failed to fetch algorithms:", err);
        }
    };
    fetchExtensions();
    fetchAlgorithms();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name as string]: value });
  };

  const handleExtensionsChange = (event: React.ChangeEvent<{ value: unknown }>) => {
    const {
      target: { value },
    } = event;
    setFormData({
      ...formData,
      extensions: typeof value === 'string' ? value.split(',') : value as string[],
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResponse(null);
    setCopySuccess(false);
    if (formData.password.length < 8) {
        setError(t('passwordLengthError'));
        return;
    }
    try {
      const res = await axios.post(`${API_BASE_URL}/api/csr`, formData);
      setResponse(res.data);
    } catch (err) {
      setError(t('errorFailedToCreateCsr'));
      console.error(err);
    }
  };

  const handleP12Download = async () => {
    if (!response) return;
    try {
      const res = await axios.get(`${API_BASE_URL}${response.privateKeyDownloadUrl}`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(res.data);
      const link = document.createElement('a');
      const filename = response.privateKeyDownloadUrl.split('/').pop();
      link.setAttribute('download', filename || 'private-key.p12');
      link.href = url;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError("Failed to download .p12 file.");
    }
  };

  const handlePemDownload = async () => {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/csr/download-pem`, formData, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      const filename = `private-key-${formData.commonName.replace(/\s+/g, '_').toLowerCase()}.pem`;
      link.setAttribute('download', filename);
      link.href = url;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError("Failed to download Encrypted PEM key.");
    }
  };

  const handlePublicKeyDownload = async () => {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/csr/download-public-key`, formData, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      const filename = `public-key-${formData.commonName.replace(/\s+/g, '_').toLowerCase()}.pem`;
      link.setAttribute('download', filename);
      link.href = url;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError("Failed to download public key.");
    }
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    });
  };

  const handleSignAndDownloadP12 = () => {
    if (response) {
      navigate('/sign', { state: { csrPem: response.csrPem, privateKeyCacheId: response.privateKeyCacheId } });
    }
  };

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('generateCsrTitle')}
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="commonName" label={t('commonName')} value={formData.commonName} onChange={handleChange} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="organization" label={t('organization')} value={formData.organization} onChange={handleChange} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="organizationalUnit" label={t('organizationalUnit')} value={formData.organizationalUnit} onChange={handleChange} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="locality" label={t('locality')} value={formData.locality} onChange={handleChange} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="state" label={t('state')} value={formData.state} onChange={handleChange} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="country" label={t('country')} value={formData.country} onChange={handleChange} inputProps={{ maxLength: 2 }} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="key-algo-csr-label">{t('keyAlgorithm')}</InputLabel>
              <Select
                labelId="key-algo-csr-label"
                name="keyAlgorithm"
                value={formData.keyAlgorithm}
                label={t('keyAlgorithm')}
                onChange={handleChange as any}
              >
                {availableAlgorithms.map((algo) => (
                  <MenuItem key={algo.id} value={algo.id}>
                    <ListItemText primary={algo.name} secondary={algo.description} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required type="password" name="password" label={t('passwordForPrivateKey')} value={formData.password} onChange={handleChange} />
          </Grid>
          <Grid item xs={12}>
            <FormControl fullWidth>
                <InputLabel id="extensions-label">Extensions</InputLabel>
                <Select
                    labelId="extensions-label"
                    multiple
                    value={formData.extensions}
                    onChange={handleExtensionsChange as any}
                    input={<OutlinedInput label="Extensions" />}
                    renderValue={(selected) => (
                        (selected as string[])
                            .map(id => availableExtensions.find(ext => ext.id === id)?.name || id)
                            .join(', ')
                    )}
                >
                    {availableExtensions.map((ext) => (
                        <MenuItem key={ext.id} value={ext.id}>
                            <Checkbox checked={formData.extensions.indexOf(ext.id) > -1} />
                            <ListItemText primary={ext.name} secondary={ext.description} />
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
          </Grid>
        </Grid>
        <Button type="submit" variant="contained" sx={{ mt: 3, mb: 2 }}>
          {t('generateCsrButton')}
        </Button>
      </Box>
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {response && (
        <Box sx={{ mt: 4 }}>
          <Alert severity="success">{t('csrCreatedSuccess')}</Alert>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 2 }}>
             <Button variant="outlined" onClick={handleP12Download}>
                {t('downloadPrivateKey')}
             </Button>
             <Button variant="outlined" onClick={handlePemDownload}>
                Download Encrypted PEM
             </Button>
             <Button variant="outlined" onClick={handlePublicKeyDownload}>
                Download Public Key
             </Button>
             <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel id="pem-algo-label">PEM Format</InputLabel>
                <Select labelId="pem-algo-label" name="pemAlgorithm" value={formData.pemAlgorithm} label="PEM Format" onChange={handleChange as any}>
                    <MenuItem value="AES_256_CBC">AES-256</MenuItem>
                    <MenuItem value="AES_128_CBC">AES-128</MenuItem>
                </Select>
             </FormControl>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
            <Typography variant="h6">{t('csrPemDisplay')}</Typography>
            <Button startIcon={<ContentCopy />} onClick={() => handleCopy(response.csrPem)} size="small">
              {copySuccess ? t('copied') : t('copy')}
            </Button>
            <Button variant="contained" onClick={handleSignAndDownloadP12} sx={{ ml: 2 }}>
              Sign with CA & Download P12
            </Button>
          </Box>
          <Paper variant="outlined" sx={{ p: 2, mt: 1, maxHeight: 300, overflow: 'auto' }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
              <code>{response.csrPem}</code>
            </pre>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default CreateCsrForm;