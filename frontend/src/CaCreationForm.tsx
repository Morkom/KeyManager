import React, { useState, useEffect } from 'react';
import {
  Box, Button, TextField, Typography, Paper, Grid,
  FormControl, InputLabel, OutlinedInput, InputAdornment, IconButton, Alert,
  Select, MenuItem, ListItemText, Checkbox
} from '@mui/material';
import { Visibility, VisibilityOff, ContentCopy } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface CaCreationFormProps {
  onCreationSuccess: () => void;
}

interface AlgorithmDto {
    id: string;
    name: string;
    description: string;
}

interface ExtensionDto {
    id: string;
    name: string;
    description: string;
}


const CaCreationForm: React.FC<CaCreationFormProps> = ({ onCreationSuccess }) => {
  const { t } = useTranslation();
  const [formData, setFormData] = useState({
    commonName: 'My Corp Root CA',
    organization: 'My Corp',
    organizationalUnit: 'IT Department',
    country: 'CZ',
    validityDays: 3650,
    keyAlgorithm: 'RSA-4096',
    password: '',
    extensions: [] as string[],
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<{ certificatePem: string; keystoreDownloadUrl: string } | null>(null);
  const [copySuccess, setCopySuccess] = useState(false);
  const [availableAlgorithms, setAvailableAlgorithms] = useState<AlgorithmDto[]>([]);
  const [availableExtensions, setAvailableExtensions] = useState<ExtensionDto[]>([]);

  useEffect(() => {
    const fetchAlgorithms = async () => {
        try {
            const res = await axios.get(`${API_BASE_URL}/api/algorithms`);
            setAvailableAlgorithms(res.data);
        } catch (err) {
            console.error("Failed to fetch algorithms:", err);
        }
    };
    const fetchExtensions = async () => {
        try {
            const res = await axios.get(`${API_BASE_URL}/api/extensions`);
            setAvailableExtensions(res.data);
            // Pre-select some common extensions for a root CA
            const defaultExtensions = res.data
                .filter(ext => ["keyCertSign", "crlSign", "subjectKeyIdentifier", "authorityKeyIdentifier"].includes(ext.id))
                .map(ext => ext.id);
            setFormData(prev => ({ ...prev, extensions: defaultExtensions }));
        } catch (err) {
            console.error("Failed to fetch extensions:", err);
        }
    };
    fetchAlgorithms();
    fetchExtensions();
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
      const res = await axios.post(`${API_BASE_URL}/api/ca`, formData);
      setResponse(res.data);
      setTimeout(() => {
        onCreationSuccess();
      }, 2000);
    } catch (err) {
      setError(t('errorFailedToCreateCa'));
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
    <Box sx={{ pt: 1 }}>
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
              name="country"
              label={t('country')}
              value={formData.country}
              onChange={handleChange}
              inputProps={{ maxLength: 2 }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              type="number"
              name="validityDays"
              label={t('validityDays')}
              value={formData.validityDays}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="key-algo-label">{t('keyAlgorithm')}</InputLabel>
              <Select
                labelId="key-algo-label"
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
          <Grid item xs={12}>
            <FormControl fullWidth variant="outlined" required>
              <InputLabel htmlFor="password">{t('keystorePassword')}</InputLabel>
              <OutlinedInput
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={handleChange}
                endAdornment={
                  <InputAdornment position="end">
                    <IconButton
                      aria-label="toggle password visibility"
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                }
                label={t('keystorePassword')}
              />
            </FormControl>
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
          {t('createCaButton')}
        </Button>
      </Box>
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {response && (
        <Box sx={{ mt: 4 }}>
          <Alert severity="success">{t('caCreatedSuccess')}</Alert>
          <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
             <Button
                variant="outlined"
                onClick={() => window.open(`${API_BASE_URL}${response.keystoreDownloadUrl}`, '_blank')}
             >
                {t('downloadKeystore')}
             </Button>
             <Button
                variant="outlined"
                onClick={() => handleDownload(response.certificatePem, 'ca-certificate.pem', 'application/x-pem-file')}
             >
                {t('downloadCert')}
             </Button>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
            <Typography variant="h6">{t('caCertPem')}</Typography>
            <Button
              startIcon={<ContentCopy />}
              onClick={() => handleCopy(response.certificatePem)}
              size="small"
            >
              {copySuccess ? t('copied') : t('copy')}
            </Button>
          </Box>
          <Paper variant="outlined" sx={{ p: 2, mt: 1, maxHeight: 300, overflow: 'auto' }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
              <code>{response.certificatePem}</code>
            </pre>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default CaCreationForm;