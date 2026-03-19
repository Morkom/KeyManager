import React, { useState, useEffect } from 'react';
import {
  Box, Button, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField, FormGroup, FormControlLabel, Checkbox, Alert
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';
import axios from 'axios';
import { useAuth } from './AuthContext';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface User {
  id: number;
  username: string;
  roles: string[];
}

const allRoles = ['ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_AUDITOR'];

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditRolesDialogOpen, setIsEditRolesDialogOpen] = useState(false);
  const [isChangePasswordDialogOpen, setIsChangePasswordDialogOpen] = useState(false);
  const [newUser, setNewUser] = useState({ username: '', password: '', roles: new Set<string>() });
  const [newPassword, setNewPassword] = useState('');

  const fetchUsers = async () => {
    try {
      const res = await axios.get<User[]>(`${API_BASE_URL}/api/users`);
      setUsers(res.data);
    } catch (err) {
      setError("Failed to load users.");
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleAddUser = async () => {
    try {
      await axios.post(`${API_BASE_URL}/api/users`, { ...newUser, roles: Array.from(newUser.roles) });
      fetchUsers();
      setIsAddDialogOpen(false);
    } catch (err) {
      setError("Failed to add user.");
    }
  };

  const handleDeleteUser = async (id: number) => {
    if (window.confirm("Are you sure you want to delete this user?")) {
      try {
        await axios.delete(`${API_BASE_URL}/api/users/${id}`);
        fetchUsers();
      } catch (err) {
        setError("Failed to delete user.");
      }
    }
  };

  const handleUpdateRoles = async () => {
    if (!selectedUser) return;
    try {
      await axios.put(`${API_BASE_URL}/api/users/${selectedUser.id}/roles`, { roles: Array.from(newUser.roles) });
      fetchUsers();
      setIsEditRolesDialogOpen(false);
    } catch (err) {
      setError("Failed to update roles.");
    }
  };

  const handleChangePassword = async () => {
    if (!selectedUser) return;
    try {
      await axios.put(`${API_BASE_URL}/api/users/${selectedUser.id}/password`, { password: newPassword });
      fetchUsers();
      setIsChangePasswordDialogOpen(false);
    } catch (err) {
      setError("Failed to change password.");
    }
  };

  const openEditRolesDialog = (user: User) => {
    setSelectedUser(user);
    setNewUser({ ...newUser, roles: new Set(user.roles) });
    setIsEditRolesDialogOpen(true);
  };

  const openChangePasswordDialog = (user: User) => {
    setSelectedUser(user);
    setNewPassword('');
    setIsChangePasswordDialogOpen(true);
  };

  const handleRoleChange = (role: string) => {
    const updatedRoles = new Set(newUser.roles);
    if (updatedRoles.has(role)) {
      updatedRoles.delete(role);
    } else {
      updatedRoles.add(role);
    }
    setNewUser({ ...newUser, roles: updatedRoles });
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" component="h2">User Management</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setIsAddDialogOpen(true)}>Add User</Button>
      </Box>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Roles</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{user.id}</TableCell>
                <TableCell>{user.username}</TableCell>
                <TableCell>{user.roles.join(', ')}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => openChangePasswordDialog(user)}>Password</Button>
                  <Button size="small" onClick={() => openEditRolesDialog(user)}>Roles</Button>
                  <IconButton onClick={() => handleDeleteUser(user.id)}><Delete /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Add User Dialog */}
      <Dialog open={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)}>
        <DialogTitle>Add New User</DialogTitle>
        <DialogContent>
          <TextField autoFocus margin="dense" label="Username" fullWidth value={newUser.username} onChange={(e) => setNewUser({ ...newUser, username: e.target.value })} />
          <TextField margin="dense" label="Password" type="password" fullWidth value={newUser.password} onChange={(e) => setNewUser({ ...newUser, password: e.target.value })} />
          <FormGroup>{allRoles.map(role => <FormControlLabel key={role} control={<Checkbox checked={newUser.roles.has(role)} onChange={() => handleRoleChange(role)} />} label={role} />)}</FormGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsAddDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddUser}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Roles Dialog */}
      <Dialog open={isEditRolesDialogOpen} onClose={() => setIsEditRolesDialogOpen(false)}>
        <DialogTitle>Edit Roles for {selectedUser?.username}</DialogTitle>
        <DialogContent>
          <FormGroup>{allRoles.map(role => <FormControlLabel key={role} control={<Checkbox checked={newUser.roles.has(role)} onChange={() => handleRoleChange(role)} />} label={role} />)}</FormGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsEditRolesDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleUpdateRoles}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Change Password Dialog */}
      <Dialog open={isChangePasswordDialogOpen} onClose={() => setIsChangePasswordDialogOpen(false)}>
        <DialogTitle>Change Password for {selectedUser?.username}</DialogTitle>
        <DialogContent>
          <TextField autoFocus margin="dense" label="New Password" type="password" fullWidth value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsChangePasswordDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleChangePassword}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default UserManagement;
