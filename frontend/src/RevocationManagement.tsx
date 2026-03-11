import React, { useState, useEffect } from 'react';
import {
  Box, Button, TextField, Typography, Grid, Alert,
  FormControl, InputLabel, Select, MenuItem, Divider,
  Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const revocationReasons = [
  { value: 0, label: 'Unspecified' },
  { value: 1, label: 'Key Compromise' },
  { value: 2, label: 'CA Compromise' },
  { value: 3, label: 'Affiliation Changed' },
  { value: 4, label: 'Superseded' },
  { value: 5, label: 'Cessation of Operation' },
  { value: 6, label: 'Certificate Hold' },
];

const RevocationManagement: React.FC = () => {
  const { t } = useTranslation();
  const [formData, setFormData] = useState({
    caFilename: '',
    caPassword: '',
    serialNumber: '',
    revocationReason: 1,
  });
  const [caList, setCaList] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [crlDialogOpen, setCrlDialogOpen] = useState(false);
  const [crlCa, setCrlCa] = useState('');
  const [crlPassword, setCrlPassword] = useState('');

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
      }
    };
    fetchCaList();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name as string]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      await axios.post(`${API_BASE_URL}/api/revocation/revoke`, formData);
      setSuccess(t('certRevokedSuccess', { serial: formData.serialNumber }));
    } catch (err) {
      setError(t('errorFailedToRevokeCert'));
      console.error(err);
    }
  };

  const handleOpenCrlDialog = (caFilename: string) => {
    setCrlCa(caFilename);
    setCrlDialogOpen(true);
  };

  const handleCrlDownload = async () => {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/revocation/crl`, {
        caFilename: crlCa,
        caPassword: crlPassword,
      }, { responseType: 'blob' });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${crlCa}.crl`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      setCrlDialogOpen(false);
      setCrlPassword('');
    } catch (err) {
      setError(t('errorFailedToDownloadCrl'));
      console.error(err);
    }
  };

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('revokeCertTitle')}
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="ca-select-label">{t('caIssuedCert')}</InputLabel>
              <Select
                labelId="ca-select-label"
                name="caFilename"
                value={formData.caFilename}
                label={t('caIssuedCert')}
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
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              required
              name="serialNumber"
              label={t('serialNumber')}
              value={formData.serialNumber}
              onChange={handleChange}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel id="reason-select-label">{t('revocationReason')}</InputLabel>
              <Select
                labelId="reason-select-label"
                name="revocationReason"
                value={formData.revocationReason}
                label={t('revocationReason')}
                onChange={handleChange as any}
              >
                {revocationReasons.map((reason) => (
                  <MenuItem key={reason.value} value={reason.value}>
                    {reason.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
        <Button type="submit" variant="contained" color="error" sx={{ mt: 3, mb: 2 }}>
          {t('revokeCertButton')}
        </Button>
      </Box>
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mt: 2 }}>{success}</Alert>}

      <Divider sx={{ my: 4 }} />

      <Typography variant="h5" component="h2" gutterBottom>
        {t('downloadCrlTitle')}
      </Typography>
      <Box>
        {caList.map(ca => (
          <Button
            key={ca}
            variant="outlined"
            sx={{ mr: 1, mb: 1 }}
            onClick={() => handleOpenCrlDialog(ca)}
          >
            {t('downloadCrlFor', { ca })}
          </Button>
        ))}
      </Box>

      <Dialog open={crlDialogOpen} onClose={() => setCrlDialogOpen(false)}>
        <DialogTitle>{t('passwordFor', { ca: crlCa })}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label={t('caKeystorePassword')}
            type="password"
            fullWidth
            variant="standard"
            value={crlPassword}
            onChange={(e) => setCrlPassword(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCrlDialogOpen(false)}>{t('cancel')}</Button>
          <Button onClick={handleCrlDownload}>{t('download')}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default RevocationManagement;
