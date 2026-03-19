import React, { useState } from 'react';
import { Box, Button, TextField, Typography, Paper, Grid, Alert, FormControl, InputLabel, Select, MenuItem } from '@mui/material';
import { ContentCopy } from '@mui/icons-material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface SshKeyResponse {
  publicKey: string;
  privateKeyPem: string;
}

const SshKeyGenerator: React.FC = () => {
  const [formData, setFormData] = useState({
    keyAlgorithm: 'RSA',
    comment: 'user@example.com',
  });
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<SshKeyResponse | null>(null);
  const [copySuccess, setCopySuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name as string]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResponse(null);
    try {
      const res = await axios.post<SshKeyResponse>(`${API_BASE_URL}/api/ssh/generate`, formData);
      setResponse(res.data);
    } catch (err) {
      setError("Failed to generate SSH key.");
    }
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    });
  };

  const handleDownload = (content: string, fileName: string) => {
    const blob = new Blob([content], { type: 'application/x-pem-file' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>
        Generate SSH Key
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel>Key Algorithm</InputLabel>
              <Select name="keyAlgorithm" value={formData.keyAlgorithm} label="Key Algorithm" onChange={handleChange as any}>
                <MenuItem value="RSA">RSA-4096</MenuItem>
                <MenuItem value="EC">ECDSA-P256</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth required name="comment" label="Comment" value={formData.comment} onChange={handleChange} />
          </Grid>
        </Grid>
        <Button type="submit" variant="contained" sx={{ mt: 3, mb: 2 }}>
          Generate Key
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

      {response && (
        <Box sx={{ mt: 4 }}>
          <Alert severity="success">SSH Key Pair Generated Successfully!</Alert>

          <Box sx={{ mt: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h6">Public Key (for GitHub/GitLab)</Typography>
              <Button startIcon={<ContentCopy />} onClick={() => handleCopy(response.publicKey)} size="small">
                {copySuccess ? 'Copied!' : 'Copy'}
              </Button>
            </Box>
            <Paper variant="outlined" sx={{ p: 2, mt: 1, overflow: 'auto' }}>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                <code>{response.publicKey}</code>
              </pre>
            </Paper>
          </Box>

          <Box sx={{ mt: 3 }}>
            <Typography variant="h6">Private Key (save this file)</Typography>
            <Button variant="outlined" sx={{ mt: 1 }} onClick={() => handleDownload(response.privateKeyPem, 'id_rsa')}>
              Download Private Key
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
};

export default SshKeyGenerator;
