// src/hooks/useAuthGuard.ts
<<<<<<< Updated upstream
import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { useAuth } from '../stores/auth';
=======
import { useEffect } from "react";
import { useNavigate } from "react-router";
import { useAuth } from "../stores/auth";
>>>>>>> Stashed changes

export default function useAuthGuard(redirectTo: string = "/") {
  const nav = useNavigate();
  const { accessToken, loading, bootstrap } = useAuth();

  useEffect(() => {
    if (loading) void bootstrap(); // refresh → me
  }, [loading, bootstrap]);

  useEffect(() => {
    if (!loading && !accessToken) {
      nav(redirectTo, { replace: true });
    }
  }, [loading, accessToken, nav, redirectTo]);
}
