import React, { useState, useEffect } from 'react';
import {
  Box, Button, TextField, Typography, Paper, Grid, Alert,
  FormControl, InputLabel, Select, MenuItem
} from '@mui/material';
import { ContentCopy } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const SignCertificateForm: React.FC = () => {
  const { t } = useTranslation();
  const [formData, setFormData] = useState({
    csrPem: '',
    caFilename: '',
    caPassword: ''
  });
  const [caList, setCaList] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<{ signedCertificatePem: string } | null>(null);
  const [copySuccess, setCopySuccess] = useState(false);

  useEffect(() => {
    const fetchCaList = async () => {
      try {
        const res = await axios.get<string[]>(`${API_BASE_URL}/api/ca/list`);
        setCaList(res.data);
        if (res.data.length > 0) {
          setFormData(prev => ({ ...prev, caFilename: res.data[0] }));
        }
      } catch (err) {
        console.error("Failed to fetch CA list", err);
        setError(t('errorFailedToFetchCaList'));
      }
    };
    fetchCaList();
  }, [t]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name as string]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResponse(null);
    setCopySuccess(false);
    try {
      const res = await axios.post(`${API_BASE_URL}/api/ca/sign`, formData);
      setResponse(res.data);
    } catch (err) {
      setError(t('errorFailedToSignCert'));
      console.error(err);
    }
  };

  const handleDownload = (content: string, fileName: string, contentType: string) => {
    const blob = new Blob([content], { type: contentType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
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
        {t('signCertTitle')}
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField
              fullWidth
              required
              multiline
              rows={10}
              name="csrPem"
              label={t('csrPem')}
              value={formData.csrPem}
              onChange={handleChange}
              variant="outlined"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="ca-select-label">{t('signingCa')}</InputLabel>
              <Select
                labelId="ca-select-label"
                name="caFilename"
                value={formData.caFilename}
                label={t('signingCa')}
                onChange={handleChange as any}
              >
                {caList.map((ca) => (
                  <MenuItem key={ca} value={ca}>
                    {ca}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              type="password"
              name="caPassword"
              label={t('caKeystorePassword')}
              value={formData.caPassword}
              onChange={handleChange}
            />
          </Grid>
        </Grid>
        <Button type="submit" variant="contained" sx={{ mt: 3, mb: 2 }}>
          {t('signCertButton')}
        </Button>
      </Box>
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {response && (
        <Box sx={{ mt: 4 }}>
          <Alert severity="success">{t('certSignedSuccess')}</Alert>
          <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
            <Button
              variant="outlined"
              onClick={() => handleDownload(response.signedCertificatePem, 'signed-certificate.pem', 'application/x-pem-file')}
            >
              {t('downloadCert')}
            </Button>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
            <Typography variant="h6">{t('signedCertPem')}</Typography>
            <Button
              startIcon={<ContentCopy />}
              onClick={() => handleCopy(response.signedCertificatePem)}
              size="small"
            >
              {copySuccess ? t('copied') : t('copy')}
            </Button>
          </Box>
          <Paper variant="outlined" sx={{ p: 2, mt: 1, maxHeight: 300, overflow: 'auto' }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
              <code>{response.signedCertificatePem}</code>
            </pre>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default SignCertificateForm;
