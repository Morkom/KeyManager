import React, { useState } from 'react';
import {
  Box, Button, TextField, Typography, Paper, Grid, Alert,
  FormControl, InputLabel, Select, MenuItem
} from '@mui/material';
import { ContentCopy } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const CreateCsrForm: React.FC = () => {
  const { t } = useTranslation();
  const [formData, setFormData] = useState({
    commonName: 'example.com',
    organization: 'My Example Corp',
    organizationalUnit: 'IT Department',
    locality: 'Brno',
    state: 'CZ',
    country: 'CZ',
    keyAlgorithm: 'RSA',
    password: ''
  });
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<{ csrPem: string; privateKeyDownloadUrl: string } | null>(null);
  const [copySuccess, setCopySuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name as string]: value });
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

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    });
  };

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('generateCsrTitle')}
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="commonName"
              label={t('commonName')}
              value={formData.commonName}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="organization"
              label={t('organization')}
              value={formData.organization}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="organizationalUnit"
              label={t('organizationalUnit')}
              value={formData.organizationalUnit}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="locality"
              label={t('locality')}
              value={formData.locality}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="state"
              label={t('state')}
              value={formData.state}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="country"
              label={t('country')}
              value={formData.country}
              onChange={handleChange}
              inputProps={{ maxLength: 2 }}
            />
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
                <MenuItem value="RSA">RSA-4096</MenuItem>
                <MenuItem value="EC">ECDSA-P384</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              type="password"
              name="password"
              label={t('passwordForPrivateKey')}
              value={formData.password}
              onChange={handleChange}
            />
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
          <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
             <Button
                variant="outlined"
                onClick={() => window.open(`${API_BASE_URL}${response.privateKeyDownloadUrl}`, '_blank')}
             >
                {t('downloadPrivateKey')}
             </Button>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
            <Typography variant="h6">{t('csrPemDisplay')}</Typography>
            <Button
              startIcon={<ContentCopy />}
              onClick={() => handleCopy(response.csrPem)}
              size="small"
            >
              {copySuccess ? t('copied') : t('copy')}
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
