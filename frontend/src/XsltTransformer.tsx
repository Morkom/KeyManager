import React, { useState } from 'react';
import { Box, Button, TextField, Typography, Paper, Grid, Alert } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const XsltTransformer: React.FC = () => {
  const [xmlText, setXmlText] = useState('<root><item>Hello</item></root>');
  const [xsltText, setXsltText] = useState(
    `<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/root">
    <html>
      <body>
        <h2>Transformed XML</h2>
        <p><xsl:value-of select="item"/></p>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>`
  );
  const [result, setResult] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setError(null);
    setResult('');

    const xmlFile = new File([xmlText], "input.xml", { type: "application/xml" });
    const xsltFile = new File([xsltText], "transform.xslt", { type: "application/xml" });

    const formData = new FormData();
    formData.append('xmlFile', xmlFile);
    formData.append('xsltFile', xsltFile);

    try {
      const res = await axios.post(`${API_BASE_URL}/api/xml/transform`, formData);
      if (res.data.error) {
        setError(res.data.error);
      } else {
        setResult(res.data.result);
      }
    } catch (err) {
      setError("An error occurred while communicating with the server.");
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>XSLT Transformer</Typography>
      <Grid container spacing={3}>
        {/* XML Input */}
        <Grid item xs={12} md={6}>
          <Typography variant="h6">XML Input</Typography>
          <TextField label="XML Content" multiline rows={15} fullWidth value={xmlText} onChange={(e) => setXmlText(e.target.value)} />
        </Grid>

        {/* XSLT Input */}
        <Grid item xs={12} md={6}>
          <Typography variant="h6">XSLT Template</Typography>
          <TextField label="XSLT Content" multiline rows={15} fullWidth value={xsltText} onChange={(e) => setXsltText(e.target.value)} />
        </Grid>
      </Grid>

      <Button variant="contained" sx={{ mt: 3 }} onClick={handleSubmit}>Transform</Button>

      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

      {result && (
        <Box sx={{ mt: 3 }}>
          <Typography variant="h6">Result</Typography>
          <TextField multiline rows={15} fullWidth value={result} InputProps={{ readOnly: true }} />
        </Box>
      )}
    </Paper>
  );
};

export default XsltTransformer;
