import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper } from '@mui/material';

const EncodingTool: React.FC = () => {
  const [input, setInput] = useState('');
  const [output, setOutput] = useState('');

  const handleEncode = (encoder: (s: string) => string) => {
    try {
      setOutput(encoder(input));
    } catch (e) {
      setOutput("Error: Invalid input for this encoding.");
    }
  };

  const handleDecode = (decoder: (s: string) => string) => {
    try {
      setOutput(decoder(input));
    } catch (e) {
      setOutput("Error: Invalid input for this decoding.");
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>Encoding / Decoding</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            label="Input"
            multiline
            rows={8}
            fullWidth
            value={input}
            onChange={(e) => setInput(e.target.value)}
            variant="outlined"
          />
        </Grid>
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
            <Button variant="contained" onClick={() => handleEncode(btoa)}>Base64 Encode</Button>
            <Button variant="outlined" onClick={() => handleDecode(atob)}>Base64 Decode</Button>
            <Button variant="contained" onClick={() => handleEncode(encodeURIComponent)}>URL Encode</Button>
            <Button variant="outlined" onClick={() => handleDecode(decodeURIComponent)}>URL Decode</Button>
          </Box>
        </Grid>
        <Grid item xs={12}>
          <TextField
            label="Output"
            multiline
            rows={8}
            fullWidth
            value={output}
            InputProps={{ readOnly: true }}
            variant="outlined"
          />
        </Grid>
      </Grid>
    </Paper>
  );
};

export default EncodingTool;
