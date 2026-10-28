import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authModalState, setAuthModalState] = useState({
    isOpen: false,
    initialTab: 'login', // 'login' | 'register'
    promptMessage: '',
  });

  // Check existing session on application mount
  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getMe();
      setUser(currentUser);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (email, password) => {
    const loggedIn = await authApi.login({ email, password });
    setUser(loggedIn);
    setAuthModalState({ isOpen: false, initialTab: 'login', promptMessage: '' });
    return loggedIn;
  };

  const register = async (formData) => {
    const registered = await authApi.register(formData);
    setUser(registered);
    setAuthModalState({ isOpen: false, initialTab: 'login', promptMessage: '' });
    return registered;
  };

  const logout = async () => {
    await authApi.logout();
    setUser(null);
  };

  const openAuthModal = (initialTab = 'login', promptMessage = '') => {
    setAuthModalState({
      isOpen: true,
      initialTab,
      promptMessage,
    });
  };

  const closeAuthModal = () => {
    setAuthModalState((prev) => ({ ...prev, isOpen: false, promptMessage: '' }));
  };

  const value = {
    user,
    isAuthenticated: Boolean(user),
    isAdmin: user?.role === 'ADMIN',
    loading,
    login,
    register,
    logout,
    refreshUser,
    authModalState,
    openAuthModal,
    closeAuthModal,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
