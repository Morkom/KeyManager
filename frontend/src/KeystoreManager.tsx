import React, { useState, useRef } from 'react';
import {
  Box, Button, TextField, Typography, Paper, Grid, Alert,
  List, ListItem, ListItemText, ListItemIcon, IconButton,
  Dialog, DialogTitle, DialogContent, Table, TableBody, TableRow, TableCell, DialogActions
} from '@mui/material';
import { VpnKey, CheckCircle, Delete, Info, Edit } from '@mui/icons-material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface KeystoreEntry {
  alias: string;
  entryType: string;
  certificateDetails: string;
}

interface CertificateDetailsDto {
  subject: Record<string, string>;
  issuer: Record<string, string>;
  serialNumber: string;
  version: number;
  validFrom: string;
  validUntil: string;
  signatureAlgorithm: string;
  publicKeyAlgorithm: string;
  sha256Fingerprint: string;
  sha1Fingerprint: string;
}

interface Modification {
  type: 'DELETE' | 'RENAME';
  alias: string;
  newAlias?: string;
}

const KeystoreManager: React.FC = () => {
  const [password, setPassword] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [entries, setEntries] = useState<KeystoreEntry[]>([]);
  const [modifications, setModifications] = useState<Modification[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<CertificateDetailsDto | null>(null);
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [isRenameOpen, setIsRenameOpen] = useState(false);
  const [renameAlias, setRenameAlias] = useState({ old: '', new: '' });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setEntries([]);
      setModifications([]);
    }
  };

  const handleView = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!file) {
      setError("Please select a keystore file.");
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);
    try {
      const res = await axios.post<KeystoreEntry[]>(`${API_BASE_URL}/api/keystore/view`, formData);
      setEntries(res.data);
      setModifications([]);
    } catch (err) {
      setError("Failed to load keystore. Check password or file format.");
    }
  };

  const handleViewEntry = async (alias: string) => {
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);
    formData.append('alias', alias);
    try {
      const res = await axios.post<CertificateDetailsDto>(`${API_BASE_URL}/api/keystore/view-entry`, formData);
      setSelectedEntry(res.data);
      setIsViewerOpen(true);
    } catch (err) {
      setError("Failed to load certificate details.");
    }
  };

  const handleDeleteEntry = (alias: string) => {
    if (window.confirm(`Mark entry '${alias}' for deletion? Changes will be applied on save.`)) {
      setEntries(entries.filter(entry => entry.alias !== alias));
      setModifications([...modifications, { type: 'DELETE', alias }]);
    }
  };

  const handleOpenRenameDialog = (oldAlias: string) => {
    setRenameAlias({ old: oldAlias, new: oldAlias });
    setIsRenameOpen(true);
  };

  const handleRenameEntry = () => {
    setEntries(entries.map(entry => entry.alias === renameAlias.old ? { ...entry, alias: renameAlias.new } : entry));
    setModifications([...modifications, { type: 'RENAME', alias: renameAlias.old, newAlias: renameAlias.new }]);
    setIsRenameOpen(false);
  };

  const handleSaveChanges = async () => {
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);

    const requestData = { password, modifications };
    formData.append('request', new Blob([JSON.stringify(requestData)], { type: 'application/json' }));

    try {
      const response = await axios.post(`${API_BASE_URL}/api/keystore/save`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `modified-${file.name}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      setModifications([]);
    } catch (err) {
      setError("Failed to save changes.");
    }
  };

  const renderDetailsTable = (details: CertificateDetailsDto) => (
    <Table size="small">
      <TableBody>
        <TableRow><TableCell colSpan={2}><Typography variant="h6">Subject</Typography></TableCell></TableRow>
        {Object.entries(details.subject).map(([k, v]) => <TableRow key={k}><TableCell>{k}</TableCell><TableCell>{v}</TableCell></TableRow>)}
        <TableRow><TableCell colSpan={2}><Typography variant="h6">Issuer</Typography></TableCell></TableRow>
        {Object.entries(details.issuer).map(([k, v]) => <TableRow key={k}><TableCell>{k}</TableCell><TableCell>{v}</TableCell></TableRow>)}
        <TableRow><TableCell colSpan={2}><Typography variant="h6">Details</Typography></TableCell></TableRow>
        <TableRow><TableCell>Serial Number</TableCell><TableCell>{details.serialNumber}</TableCell></TableRow>
        <TableRow><TableCell colSpan={2}><Typography variant="h6">Fingerprints</Typography></TableCell></TableRow>
        <TableRow><TableCell>SHA-256</TableCell><TableCell sx={{ wordBreak: 'break-all' }}>{details.sha256Fingerprint}</TableCell></TableRow>
      </TableBody>
    </Table>
  );

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>Keystore Manager</Typography>
      <Box component="form" onSubmit={handleView} noValidate>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6}><Button variant="outlined" fullWidth onClick={() => fileInputRef.current?.click()}>{file ? `Selected: ${file.name}` : "Select Keystore File"}</Button><input type="file" ref={fileInputRef} hidden accept=".jks,.p12,.pfx" onChange={handleFileChange} /></Grid>
          <Grid item xs={12} sm={6}><TextField fullWidth required type="password" label="Keystore Password" value={password} onChange={(e) => setPassword(e.target.value)} /></Grid>
        </Grid>
        <Button type="submit" variant="contained" sx={{ mt: 3, mb: 2 }}>View Contents</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

      {entries.length > 0 && (
        <Box sx={{ mt: 4 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" gutterBottom>Keystore Entries</Typography>
            <Button variant="contained" color="primary" disabled={modifications.length === 0} onClick={handleSaveChanges}>Save Changes</Button>
          </Box>
          <List>
            {entries.map((entry) => (
              <Paper key={entry.alias} sx={{ mb: 1 }} variant="outlined">
                <ListItem
                  secondaryAction={
                    <>
                      <IconButton edge="end" onClick={() => handleViewEntry(entry.alias)}><Info /></IconButton>
                      <IconButton edge="end" onClick={() => handleOpenRenameDialog(entry.alias)}><Edit /></IconButton>
                      <IconButton edge="end" onClick={() => handleDeleteEntry(entry.alias)}><Delete /></IconButton>
                    </>
                  }
                >
                  <ListItemIcon>{entry.entryType === 'Key Pair' ? <VpnKey /> : <CheckCircle />}</ListItemIcon>
                  <ListItemText primary={entry.alias} secondary={entry.certificateDetails} />
                </ListItem>
              </Paper>
            ))}
          </List>
        </Box>
      )}

      <Dialog open={isViewerOpen} onClose={() => setIsViewerOpen(false)} maxWidth="md" fullWidth><DialogTitle>Certificate Details</DialogTitle><DialogContent>{selectedEntry && renderDetailsTable(selectedEntry)}</DialogContent><DialogActions><Button onClick={() => setIsViewerOpen(false)}>Close</Button></DialogActions></Dialog>
      <Dialog open={isRenameOpen} onClose={() => setIsRenameOpen(false)}><DialogTitle>Rename Alias</DialogTitle><DialogContent><TextField autoFocus margin="dense" label="New Alias" type="text" fullWidth variant="standard" value={renameAlias.new} onChange={(e) => setRenameAlias({ ...renameAlias, new: e.target.value })} /></DialogContent><DialogActions><Button onClick={() => setIsRenameOpen(false)}>Cancel</Button><Button onClick={handleRenameEntry}>Rename</Button></DialogActions></Dialog>
    </Box>
  );
};

export default KeystoreManager;
