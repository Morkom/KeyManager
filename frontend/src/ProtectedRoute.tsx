import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';

const AUTH_ENABLED = import.meta.env.VITE_AUTH_ENABLED != 'no-auth';

const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();

  // If auth is not enabled for this environment, always allow access.
  if (!AUTH_ENABLED) {
    return <Outlet />;
  }

  // If auth is enabled, check if the user is authenticated.
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
