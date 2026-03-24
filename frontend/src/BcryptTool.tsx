import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper, Alert, Divider } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const BcryptTool: React.FC = () => {
  const [hashInput, setHashInput] = useState('');
  const [generatedHash, setGeneratedHash] = useState('');
  const [verifyPassword, setVerifyPassword] = useState('');
  const [verifyHash, setVerifyHash] = useState('');
  const [verifyResult, setVerifyResult] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleHash = async () => {
    setError(null);
    try {
      const res = await axios.post(`${API_BASE_URL}/api/crypto/bcrypt/hash`, { password: hashInput });
      setGeneratedHash(res.data.hash);
    } catch (e) {
      setError("Failed to generate hash.");
    }
  };

  const handleVerify = async () => {
    setError(null);
    try {
      const res = await axios.post(`${API_BASE_URL}/api/crypto/bcrypt/verify`, { password: verifyPassword, hash: verifyHash });
      setVerifyResult(res.data.matches);
    } catch (e) {
      setError("Failed to verify hash.");
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>Bcrypt Hash Generator & Verifier</Typography>

      {/* Hashing Section */}
      <Box component="form" onSubmit={(e) => { e.preventDefault(); handleHash(); }}>
        <Typography variant="subtitle1" sx={{ mt: 2 }}>Generate Hash</Typography>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={8}>
            <TextField
              label="Password to Hash"
              type="password"
              fullWidth
              value={hashInput}
              onChange={(e) => setHashInput(e.target.value)}
              variant="outlined"
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <Button variant="contained" fullWidth onClick={handleHash}>Generate</Button>
          </Grid>
          {generatedHash && (
            <Grid item xs={12}>
              <TextField
                label="Generated Bcrypt Hash"
                multiline
                fullWidth
                value={generatedHash}
                InputProps={{ readOnly: true }}
                variant="filled"
              />
            </Grid>
          )}
        </Grid>
      </Box>

      <Divider sx={{ my: 4 }} />

      {/* Verification Section */}
      <Box component="form" onSubmit={(e) => { e.preventDefault(); handleVerify(); }}>
        <Typography variant="subtitle1">Verify Password</Typography>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField
              label="Plain Text Password"
              type="password"
              fullWidth
              value={verifyPassword}
              onChange={(e) => setVerifyPassword(e.target.value)}
              variant="outlined"
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Bcrypt Hash to Verify Against"
              multiline
              fullWidth
              value={verifyHash}
              onChange={(e) => setVerifyHash(e.target.value)}
              variant="outlined"
            />
          </Grid>
          <Grid item xs={12}>
            <Button variant="outlined" fullWidth onClick={handleVerify}>Verify</Button>
          </Grid>
          {verifyResult !== null && (
            <Grid item xs={12}>
              <Alert severity={verifyResult ? "success" : "error"}>
                {verifyResult ? "Passwords Match" : "Passwords Do Not Match"}
              </Alert>
            </Grid>
          )}
        </Grid>
      </Box>
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
    </Paper>
  );
};

export default BcryptTool;
