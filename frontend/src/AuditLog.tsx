import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip } from '@mui/material';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface AuditEvent {
  id: number;
  timestamp: string;
  username: string;
  action: string;
  details: string;
  success: boolean;
}

const AuditLog: React.FC = () => {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        const res = await axios.get<AuditEvent[]>(`${API_BASE_URL}/api/audit`);
        // Sort events descending by timestamp
        setEvents(res.data.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()));
      } catch (err) {
        setError("Failed to load audit log.");
        console.error(err);
      }
    };
    fetchEvents();
  }, []);

  return (
    <Box>
      <Typography variant="h5" component="h2" gutterBottom>
        Audit Log
      </Typography>
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="audit log table">
          <TableHead>
            <TableRow>
              <TableCell>Timestamp</TableCell>
              <TableCell>User</TableCell>
              <TableCell>Action</TableCell>
              <TableCell>Details</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {events.map((event) => (
              <TableRow key={event.id}>
                <TableCell>{new Date(event.timestamp).toLocaleString()}</TableCell>
                <TableCell>{event.username}</TableCell>
                <TableCell>{event.action}</TableCell>
                <TableCell>{event.details}</TableCell>
                <TableCell>
                  <Chip label={event.success ? "Success" : "Failure"} color={event.success ? "success" : "error"} size="small" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default AuditLog;
