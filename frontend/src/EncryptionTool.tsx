import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper, Alert } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const EncryptionTool: React.FC = () => {
  const [input, setInput] = useState('');
  const [password, setPassword] = useState('');
  const [output, setOutput] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleCrypto = async (endpoint: 'encrypt' | 'decrypt') => {
    setError(null);
    try {
      // We must Base64 encode the input string before sending it to the backend
      const base64Input = btoa(input);
      const res = await axios.post(`${API_BASE_URL}/api/crypto/${endpoint}`, { data: base64Input, password });
      // The backend returns Base64 data, so we decode it for display
      setOutput(atob(res.data.data));
    } catch (e) {
      setOutput('');
      setError(`Operation failed. Check your input or password.`);
      console.error(e);
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>AES-256-GCM Encryption / Decryption</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            label="Input / Output"
            multiline
            rows={8}
            fullWidth
            value={input}
            onChange={(e) => setInput(e.target.value)}
            variant="outlined"
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            label="Password / Key"
            type="password"
            fullWidth
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            variant="outlined"
          />
        </Grid>
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button variant="contained" onClick={() => handleCrypto('encrypt')}>Encrypt</Button>
            <Button variant="outlined" onClick={() => handleCrypto('decrypt')}>Decrypt</Button>
          </Box>
        </Grid>
        <Grid item xs={12}>
          <TextField
            label="Result"
            multiline
            rows={8}
            fullWidth
            value={output}
            InputProps={{ readOnly: true }}
            variant="outlined"
          />
        </Grid>
        {error && <Grid item xs={12}><Alert severity="error">{error}</Alert></Grid>}
      </Grid>
    </Paper>
  );
};

export default EncryptionTool;
