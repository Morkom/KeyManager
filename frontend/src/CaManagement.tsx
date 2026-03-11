import React, { useState, useEffect, useRef } from 'react';
import {
  Box, Button, Typography, List, ListItem, ListItemText, IconButton, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import { Delete, Add, Upload } from '@mui/icons-material';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import CaCreationForm from './CaCreationForm';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const CaManagement: React.FC = () => {
  const { t } = useTranslation();
  const [caList, setCaList] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
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
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => setOpenCreateDialog(true)}
            sx={{ mr: 1 }}
          >
            {t('createNew')}
          </Button>
          <Button
            variant="outlined"
            startIcon={<Upload />}
            onClick={() => fileInputRef.current?.click()}
          >
            {t('upload')}
          </Button>
          <input
            type="file"
            ref={fileInputRef}
            hidden
            accept=".p12"
            onChange={handleUpload}
          />
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <List>
        {caList.length === 0 && !error ? (
          <ListItem>
            <ListItemText primary={t('noCaFound')} />
          </ListItem>
        ) : (
          caList.map((ca) => (
            <ListItem
              key={ca}
              secondaryAction={
                <IconButton edge="end" aria-label="delete" onClick={() => handleDelete(ca)}>
                  <Delete />
                </IconButton>
              }
            >
              <ListItemText primary={ca} />
            </ListItem>
          ))
        )}
      </List>

      <Dialog open={openCreateDialog} onClose={() => setOpenCreateDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>{t('createCaTitle')}</DialogTitle>
        <DialogContent>
          <CaCreationForm onCreationSuccess={handleCreationSuccess} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateDialog(false)}>{t('cancel')}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CaManagement;
