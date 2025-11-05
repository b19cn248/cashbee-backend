/**
 * CashBee API Client
 *
 * Ready-to-use API client for Frontend developers
 * Copy this file to your project and update BASE_URL
 *
 * @version 1.0
 * @date 2025-11-03
 */

import axios, { AxiosInstance, AxiosError } from 'axios';
import type {
  ApiResponse,
  ImportOrdersResponse,
  WalletResponse,
  AffiliateOrderResponse,
  TransactionResponse,
  UserResponse
} from './types';

// ============================================
// CONFIGURATION
// ============================================

const BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

// ============================================
// AXIOS INSTANCE
// ============================================

/**
 * Create axios instance with default config
 */
const createApiClient = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: BASE_URL,
    timeout: 30000, // 30 seconds
    headers: {
      'Content-Type': 'application/json'
    }
  });

  // Request interceptor - Add auth token
  instance.interceptors.request.use(
    (config) => {
      const token = getAuthToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // Response interceptor - Handle errors
  instance.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      handleApiError(error);
      return Promise.reject(error);
    }
  );

  return instance;
};

const apiClient = createApiClient();

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Get auth token from localStorage
 */
function getAuthToken(): string | null {
  return localStorage.getItem('authToken');
}

/**
 * Set auth token to localStorage
 */
export function setAuthToken(token: string): void {
  localStorage.setItem('authToken', token);
}

/**
 * Remove auth token from localStorage
 */
export function clearAuthToken(): void {
  localStorage.removeItem('authToken');
}

/**
 * Handle API errors globally
 */
function handleApiError(error: AxiosError): void {
  if (error.response?.status === 401) {
    // Unauthorized - redirect to login
    console.error('Authentication required');
    clearAuthToken();
    window.location.href = '/login';
  } else if (error.response?.status === 403) {
    // Forbidden
    console.error('Insufficient permissions');
  } else if (error.response?.status === 500) {
    // Server error
    console.error('Server error:', error.response.data);
  } else if (error.code === 'ECONNABORTED') {
    // Timeout
    console.error('Request timeout');
  } else if (!error.response) {
    // Network error
    console.error('Network error');
  }
}

// ============================================
// API METHODS - IMPORT
// ============================================

/**
 * Upload CSV file to import orders
 *
 * @param file - CSV file from Shopee
 * @param adminUserId - Admin user ID
 * @param options - Import options
 * @returns Import result
 *
 * @example
 * const result = await importOrders(file, 1, {
 *   skipDuplicates: true,
 *   autoMatch: true
 * });
 */
export async function importOrders(
  file: File,
  adminUserId: number,
  options: {
    platformCode?: string;
    skipDuplicates?: boolean;
    autoMatch?: boolean;
  } = {}
): Promise<ImportOrdersResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('platformCode', options.platformCode || 'shopee');
  formData.append('importedBy', adminUserId.toString());
  formData.append('skipDuplicates', (options.skipDuplicates !== false).toString());
  formData.append('autoMatch', (options.autoMatch !== false).toString());

  const response = await apiClient.post<ApiResponse<ImportOrdersResponse>>(
    '/api/admin/import/orders',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Import failed');
  }

  return response.data.data;
}

/**
 * Upload CSV with progress tracking
 *
 * @param file - CSV file
 * @param adminUserId - Admin user ID
 * @param onProgress - Progress callback (0-100)
 * @returns Import result
 *
 * @example
 * const result = await importOrdersWithProgress(file, 1, (progress) => {
 *   console.log(`Upload: ${progress}%`);
 *   setUploadProgress(progress);
 * });
 */
export async function importOrdersWithProgress(
  file: File,
  adminUserId: number,
  onProgress: (progress: number) => void
): Promise<ImportOrdersResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('platformCode', 'shopee');
  formData.append('importedBy', adminUserId.toString());
  formData.append('skipDuplicates', 'true');
  formData.append('autoMatch', 'true');

  const response = await apiClient.post<ApiResponse<ImportOrdersResponse>>(
    '/api/admin/import/orders',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(percentCompleted);
        }
      }
    }
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Import failed');
  }

  return response.data.data;
}

/**
 * Get import batch details
 *
 * @param batchId - Import batch ID
 * @returns Batch details
 */
export async function getImportBatch(batchId: number): Promise<any> {
  const response = await apiClient.get<ApiResponse<any>>(
    `/api/admin/import/batches/${batchId}`
  );

  if (!response.data.success) {
    throw new Error(response.data.message || 'Failed to get batch');
  }

  return response.data.data;
}

/**
 * Get recent import batches
 *
 * @param limit - Number of batches to return
 * @returns List of batches
 */
export async function getRecentImportBatches(limit: number = 10): Promise<any[]> {
  const response = await apiClient.get<ApiResponse<any>>(
    '/api/admin/import/batches',
    {
      params: { limit }
    }
  );

  if (!response.data.success) {
    throw new Error(response.data.message || 'Failed to get batches');
  }

  return response.data.data || [];
}

// ============================================
// API METHODS - WALLET
// ============================================

/**
 * Get user wallet
 *
 * @param userId - User ID
 * @returns Wallet information
 *
 * @example
 * const wallet = await getWallet(1);
 * console.log('Balance:', wallet.balance);
 * console.log('Pending:', wallet.pendingBalance);
 */
export async function getWallet(userId: number): Promise<WalletResponse> {
  const response = await apiClient.get<ApiResponse<WalletResponse>>(
    `/api/wallets/user/${userId}`
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to get wallet');
  }

  return response.data.data;
}

// ============================================
// API METHODS - ORDERS
// ============================================

/**
 * Get current user's orders
 *
 * @param userId - User ID (will be from JWT in production)
 * @returns List of orders
 *
 * @example
 * const orders = await getMyOrders(1);
 * console.log(`You have ${orders.length} orders`);
 */
export async function getMyOrders(userId: number): Promise<AffiliateOrderResponse[]> {
  const response = await apiClient.get<ApiResponse<AffiliateOrderResponse[]>>(
    '/api/orders/my',
    {
      params: { userId }
    }
  );

  if (!response.data.success) {
    throw new Error(response.data.message || 'Failed to get orders');
  }

  return response.data.data || [];
}

/**
 * Get specific order by ID
 *
 * @param orderId - Order ID
 * @returns Order details
 *
 * @example
 * const order = await getOrderById(123);
 * console.log('Cashback:', order.cashbackAmount);
 */
export async function getOrderById(orderId: number): Promise<AffiliateOrderResponse> {
  const response = await apiClient.get<ApiResponse<AffiliateOrderResponse>>(
    `/api/orders/${orderId}`
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Order not found');
  }

  return response.data.data;
}

// ============================================
// API METHODS - TRANSACTIONS
// ============================================

/**
 * Get transaction history
 *
 * @param userId - User ID
 * @param page - Page number (0-indexed)
 * @param size - Page size
 * @returns List of transactions
 *
 * @example
 * const transactions = await getTransactions(1, 0, 20);
 * console.log(`Found ${transactions.length} transactions`);
 */
export async function getTransactions(
  userId: number,
  page: number = 0,
  size: number = 20
): Promise<TransactionResponse[]> {
  const response = await apiClient.get<ApiResponse<TransactionResponse[]>>(
    `/api/transactions/user/${userId}`,
    {
      params: { page, size }
    }
  );

  if (!response.data.success) {
    throw new Error(response.data.message || 'Failed to get transactions');
  }

  return response.data.data || [];
}

// ============================================
// API METHODS - USER
// ============================================

/**
 * Sync user from Keycloak
 *
 * @param keycloakId - Keycloak user ID
 * @param email - User email
 * @param fullName - User full name
 * @returns User data
 *
 * @example
 * const user = await syncUser('keycloak-123', 'user@example.com', 'John Doe');
 */
export async function syncUser(
  keycloakId: string,
  email: string,
  fullName: string
): Promise<UserResponse> {
  const response = await apiClient.post<ApiResponse<UserResponse>>(
    '/api/users/sync',
    {
      keycloakId,
      email,
      fullName
    }
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to sync user');
  }

  return response.data.data;
}

/**
 * Get user by Keycloak ID
 *
 * @param keycloakId - Keycloak user ID
 * @returns User data
 */
export async function getUserByKeycloakId(keycloakId: string): Promise<UserResponse> {
  const response = await apiClient.get<ApiResponse<UserResponse>>(
    `/api/users/keycloak/${keycloakId}`
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'User not found');
  }

  return response.data.data;
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

/**
 * Validate CSV file before upload
 *
 * @param file - File to validate
 * @throws Error if validation fails
 *
 * @example
 * try {
 *   validateCSVFile(file);
 *   console.log('File is valid');
 * } catch (error) {
 *   alert(error.message);
 * }
 */
export function validateCSVFile(file: File | null): void {
  if (!file) {
    throw new Error('Please select a file');
  }

  if (!file.name.toLowerCase().endsWith('.csv')) {
    throw new Error('Only CSV files are allowed');
  }

  if (file.size === 0) {
    throw new Error('File is empty');
  }

  const maxSize = 10 * 1024 * 1024; // 10MB
  if (file.size > maxSize) {
    throw new Error('File size must be less than 10MB');
  }
}

/**
 * Format file size to human-readable string
 *
 * @param bytes - File size in bytes
 * @returns Formatted string
 *
 * @example
 * formatFileSize(1024) // "1.00 KB"
 * formatFileSize(1048576) // "1.00 MB"
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(2)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// ============================================
// EXPORT DEFAULT CLIENT
// ============================================

export default {
  // Import
  importOrders,
  importOrdersWithProgress,
  getImportBatch,
  getRecentImportBatches,

  // Wallet
  getWallet,

  // Orders
  getMyOrders,
  getOrderById,

  // Transactions
  getTransactions,

  // User
  syncUser,
  getUserByKeycloakId,

  // Auth
  setAuthToken,
  clearAuthToken,
  getAuthToken,

  // Utilities
  validateCSVFile,
  formatFileSize
};
