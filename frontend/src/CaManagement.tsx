import React, { useState, useEffect, useRef } from 'react';
import {
  Box, Button, Typography, List, ListItem, ListItemText, IconButton, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField
} from '@mui/material';
import { Delete, Add, Upload, Download, Public } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import CaCreationForm from './CaCreationForm';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const CaManagement: React.FC = () => {
  const { t } = useTranslation();
  const [caList, setCaList] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [openPasswordDialog, setOpenPasswordDialog] = useState(false);
  const [selectedCa, setSelectedCa] = useState('');
  const [caPassword, setCaPassword] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchCaList = async () => {
    try {
      const res = await axios.get<string[]>(`${API_BASE_URL}/api/ca/list`);
      setCaList(res.data);
    } catch (err) {
      console.error("Failed to fetch CA list", err);
      setError(t('errorFailedToFetchCaList'));
    }
  };

  useEffect(() => {
    fetchCaList();
  }, [t]);

  const handleDelete = async (filename: string) => {
    if (window.confirm(t('deleteConfirm', { filename }))) {
      try {
        await axios.delete(`${API_BASE_URL}/api/ca/${filename}`);
        fetchCaList();
      } catch (err) {
        console.error("Failed to delete CA", err);
        setError(t('errorFailedToDeleteCa', { filename }));
      }
    }
  };

  const handleDownload = async (filename: string) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/ca/download/${filename}`, {
        responseType: 'blob',
      });
      // **THE FIX:** Use response.data directly as it's already a Blob
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url); // Clean up the object URL
    } catch (err) {
      setError("Failed to download file.");
      console.error(err);
    }
  };

  const handleOpenPasswordDialog = (filename: string) => {
    setSelectedCa(filename);
    setOpenPasswordDialog(true);
  };

  const handlePublicCertDownload = async () => {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/ca/download-public`,
        { caFilename: selectedCa, caPassword: caPassword },
        { responseType: 'blob' }
      );
      // **THE FIX:** Use response.data directly as it's already a Blob
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      const pemFilename = selectedCa.replace('.p12', '.pem');
      link.setAttribute('download', pemFilename);
      link.href = url; // Set href for the link
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url); // Clean up the object URL
      setOpenPasswordDialog(false);
      setCaPassword('');
    } catch (err) {
      setError("Failed to download public certificate. Is the password correct?");
    }
  };

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      await axios.post(`${API_BASE_URL}/api/ca/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      fetchCaList();
    } catch (err) {
      console.error("Failed to upload keystore", err);
      setError(t('errorFailedToUpload'));
    }
  };

  const handleCreationSuccess = () => {
    setOpenCreateDialog(false);
    fetchCaList();
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" component="h2">
          {t('manageCaTitle')}
        </Typography>
        <Box>
          <Button variant="contained" startIcon={<Add />} onClick={() => setOpenCreateDialog(true)} sx={{ mr: 1 }}>
            {t('createNew')}
          </Button>
          <Button variant="outlined" startIcon={<Upload />} onClick={() => fileInputRef.current?.click()}>
            {t('upload')}
          </Button>
          <input type="file" ref={fileInputRef} hidden accept=".p12" onChange={handleUpload} />
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <List>
        {caList.map((ca) => (
          <ListItem
            key={ca}
            secondaryAction={
              <>
                <IconButton edge="end" title="Download Keystore (.p12)" onClick={() => handleDownload(ca)}><Download /></IconButton>
                <IconButton edge="end" title="Download Public Certificate (.pem)" onClick={() => handleOpenPasswordDialog(ca)}><Public /></IconButton>
                <IconButton edge="end" title="Delete" onClick={() => handleDelete(ca)}><Delete /></IconButton>
              </>
            }
          >
            <ListItemText primary={ca} />
          </ListItem>
        ))}
      </List>

      <Dialog open={openCreateDialog} onClose={() => setOpenCreateDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>{t('createCaTitle')}</DialogTitle>
        <DialogContent><CaCreationForm onCreationSuccess={handleCreationSuccess} /></DialogContent>
        <DialogActions><Button onClick={() => setOpenCreateDialog(false)}>{t('cancel')}</Button></DialogActions>
      </Dialog>

      <Dialog open={openPasswordDialog} onClose={() => setOpenPasswordDialog(false)}>
        <DialogTitle>Enter Password for {selectedCa}</DialogTitle>
        <DialogContent>
          <TextField autoFocus margin="dense" label="Keystore Password" type="password" fullWidth variant="standard" value={caPassword} onChange={(e) => setCaPassword(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenPasswordDialog(false)}>Cancel</Button>
          <Button onClick={handlePublicCertDownload}>Download</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CaManagement;
