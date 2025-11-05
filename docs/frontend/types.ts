/**
 * TypeScript Type Definitions for CashBee API
 *
 * Copy these types to your frontend project to get type safety
 * when working with CashBee Backend API
 *
 * @version 1.0
 * @date 2025-11-03
 */

// ============================================
// COMMON TYPES
// ============================================

/**
 * Standard API Response Wrapper
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errorCode?: string;
  timestamp: string;
}

/**
 * Error Response
 */
export interface ErrorResponse {
  success: false;
  data: null;
  message: string;
  errorCode: string;
  timestamp: string;
}

// ============================================
// IMPORT & CASHBACK TYPES
// ============================================

/**
 * Import Status Enum
 */
export type ImportStatus = 'PROCESSING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';

/**
 * Order Status Enum
 */
export type OrderStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAID';

/**
 * Cashback Status Enum
 */
export type CashbackStatus = 'PENDING' | 'CONFIRMED' | 'PAID' | 'CANCELLED';

/**
 * Transaction Type Enum
 */
export type TransactionType = 'CASHBACK' | 'WITHDRAWAL' | 'REFUND' | 'ADJUSTMENT';

/**
 * Transaction Status Enum
 */
export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

/**
 * Import Orders Response
 *
 * Returned after uploading CSV file
 */
export interface ImportOrdersResponse {
  /** Import batch ID for tracking */
  batchId: number;

  /** Platform name (e.g., "Shopee") */
  platformName: string;

  /** Platform code (e.g., "shopee") */
  platformCode: string;

  /** Uploaded file name */
  fileName: string;

  /** Import status */
  status: ImportStatus;

  /** Total rows in CSV */
  totalRows: number;

  /** Number of successfully imported orders */
  successCount: number;

  /** Number of failed imports */
  failedCount: number;

  /** Number of skipped orders (duplicates, cancelled) */
  skippedCount: number;

  /** Number of orders matched with clicks */
  matchedCount: number;

  /** Success rate percentage (0-100) */
  successRate: number;

  /** List of import errors */
  errors: ImportError[];

  /** When import started */
  startedAt: string; // ISO 8601 date string

  /** When import completed */
  completedAt: string; // ISO 8601 date string

  /** Import duration in seconds */
  durationSeconds: number;

  /** Admin user ID who imported */
  importedBy: number;

  /** Human-readable message */
  message: string;

  /** Error message if status is FAILED */
  errorMessage?: string;
}

/**
 * Import Error Detail
 */
export interface ImportError {
  /** Row number in CSV (1-based) */
  rowNumber: number;

  /** Order ID that failed */
  orderId: string;

  /** Error message */
  error: string;

  /** Raw CSV data for this row */
  rawData: string;
}

// ============================================
// WALLET TYPES
// ============================================

/**
 * User Wallet Response
 *
 * Contains user's balance information
 */
export interface WalletResponse {
  /** Wallet ID */
  id: number;

  /** User ID */
  userId: number;

  /** Available balance (can withdraw) in VND */
  balance: number;

  /** Pending balance (waiting for order completion) in VND */
  pendingBalance: number;

  /** Locked balance (payout in progress) in VND */
  lockedBalance: number;

  /** Total earned (balance + pending + locked) in VND */
  totalEarned: number;

  /** Total withdrawn in VND */
  totalWithdrawn: number;

  /** When wallet was created */
  createdAt: string; // ISO 8601

  /** When wallet was last updated */
  updatedAt: string; // ISO 8601
}

// ============================================
// ORDER TYPES
// ============================================

/**
 * Affiliate Order Response
 *
 * Represents a single affiliate order
 */
export interface AffiliateOrderResponse {
  /** Order ID (internal) */
  id: number;

  /** Order ID from platform (e.g., Shopee order ID) */
  orderId: string;

  /** User ID who will receive cashback */
  userId: number;

  /** Platform ID (1 = Shopee) */
  platformId: number;

  /** Platform name */
  platformName: string;

  /** Click ID (tracking code) */
  clickId?: string;

  /** Product name */
  productName: string;

  /** Product price in VND */
  productPrice: number;

  /** Commission amount from platform in VND */
  commissionAmount: number;

  /** Cashback amount for user in VND */
  cashbackAmount: number;

  /** Currency code */
  currency: string;

  /** When order was placed */
  orderTime: string; // ISO 8601

  /** Order status */
  orderStatus: OrderStatus;

  /** Cashback status */
  cashbackStatus: CashbackStatus;

  /** Order source (IMPORT, API, MANUAL) */
  source: string;

  /** Import batch ID if from CSV import */
  importBatchId?: number;

  /** When record was created */
  createdAt: string; // ISO 8601

  /** When record was updated */
  updatedAt: string; // ISO 8601
}

// ============================================
// TRANSACTION TYPES
// ============================================

/**
 * Transaction Response
 *
 * Represents a wallet transaction
 */
export interface TransactionResponse {
  /** Transaction ID */
  id: number;

  /** User ID */
  userId: number;

  /** Transaction type */
  type: TransactionType;

  /** Transaction amount (positive = credit, negative = debit) */
  amount: number;

  /** Balance before transaction */
  balanceBefore: number;

  /** Balance after transaction */
  balanceAfter: number;

  /** Transaction status */
  status: TransactionStatus;

  /** Human-readable description */
  description: string;

  /** Reference ID (e.g., ORDER_123, PAYOUT_456) */
  referenceId?: string;

  /** When transaction was created */
  createdAt: string; // ISO 8601
}

// ============================================
// USER TYPES
// ============================================

/**
 * User Response
 */
export interface UserResponse {
  /** User ID (internal) */
  id: number;

  /** Keycloak user ID */
  keycloakId: string;

  /** Email */
  email: string;

  /** Full name */
  fullName: string;

  /** Phone number */
  phoneNumber?: string;

  /** User level */
  userLevel: 'NORMAL' | 'VIP' | 'PREMIUM';

  /** When user was created */
  createdAt: string; // ISO 8601

  /** When user was last updated */
  updatedAt: string; // ISO 8601
}

// ============================================
// API REQUEST TYPES
// ============================================

/**
 * Upload CSV Request
 *
 * Use FormData to send this request
 */
export interface ImportOrdersRequest {
  /** CSV file */
  file: File;

  /** Platform code (default: "shopee") */
  platformCode?: string;

  /** Admin user ID who is importing */
  importedBy: number;

  /** Skip duplicate orders (default: true) */
  skipDuplicates?: boolean;

  /** Auto-match orders with clicks (default: true) */
  autoMatch?: boolean;
}

/**
 * Add Pending Balance Command
 */
export interface AddPendingBalanceCommand {
  /** User ID */
  userId: number;

  /** Amount to add */
  amount: number;

  /** Description */
  description: string;
}

/**
 * Confirm Pending Balance Command
 */
export interface ConfirmPendingBalanceCommand {
  /** User ID */
  userId: number;

  /** Amount to confirm */
  amount: number;

  /** Description */
  description: string;
}

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Format money amount to Vietnamese Dong
 *
 * @example
 * formatMoney(100000) // "100.000"
 * formatMoney(1500000) // "1.500.000"
 */
export function formatMoney(amount: number): string {
  return new Intl.NumberFormat('vi-VN').format(amount);
}

/**
 * Format date/time to Vietnamese locale
 *
 * @example
 * formatDateTime("2025-11-03T22:50:00") // "03/11/2025 22:50:00"
 */
export function formatDateTime(dateString: string): string {
  return new Date(dateString).toLocaleString('vi-VN');
}

/**
 * Format date to Vietnamese locale (date only)
 *
 * @example
 * formatDate("2025-11-03T22:50:00") // "03/11/2025"
 */
export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('vi-VN');
}

/**
 * Get order status badge config
 */
export function getOrderStatusConfig(status: OrderStatus) {
  const configs = {
    APPROVED: { label: 'Đã hoàn thành', color: 'green', icon: '✅' },
    PENDING: { label: 'Đang xử lý', color: 'yellow', icon: '⏳' },
    REJECTED: { label: 'Đã hủy', color: 'red', icon: '❌' },
    PAID: { label: 'Đã thanh toán', color: 'blue', icon: '💰' }
  };

  return configs[status] || configs.PENDING;
}

/**
 * Get cashback status badge config
 */
export function getCashbackStatusConfig(status: CashbackStatus) {
  const configs = {
    PAID: { label: 'Đã nhận tiền', color: 'green', icon: '✅' },
    CONFIRMED: { label: 'Đã xác nhận', color: 'blue', icon: '👍' },
    PENDING: { label: 'Đang chờ', color: 'yellow', icon: '⏳' },
    CANCELLED: { label: 'Đã hủy', color: 'red', icon: '❌' }
  };

  return configs[status] || configs.PENDING;
}

/**
 * Get transaction type icon
 */
export function getTransactionIcon(type: TransactionType): string {
  const icons = {
    CASHBACK: '💰',
    WITHDRAWAL: '🏦',
    REFUND: '↩️',
    ADJUSTMENT: '⚙️'
  };

  return icons[type] || '📄';
}

/**
 * Get import status badge config
 */
export function getImportStatusConfig(status: ImportStatus) {
  const configs = {
    COMPLETED: { label: 'Hoàn thành', color: 'green', icon: '✅' },
    PARTIAL: { label: 'Một phần', color: 'yellow', icon: '⚠️' },
    FAILED: { label: 'Thất bại', color: 'red', icon: '❌' },
    PROCESSING: { label: 'Đang xử lý', color: 'blue', icon: '⏳' }
  };

  return configs[status] || configs.PROCESSING;
}

// ============================================
// ERROR CODES
// ============================================

/**
 * Common Error Codes
 */
export enum ErrorCode {
  // File errors
  INVALID_FILE = 'INVALID_FILE',
  INVALID_FILE_TYPE = 'INVALID_FILE_TYPE',
  FILE_READ_ERROR = 'FILE_READ_ERROR',
  FILE_TOO_LARGE = 'FILE_TOO_LARGE',

  // Auth errors
  UNAUTHORIZED = 'UNAUTHORIZED',
  INVALID_TOKEN = 'INVALID_TOKEN',
  FORBIDDEN = 'FORBIDDEN',

  // Business logic errors
  NOT_FOUND = 'NOT_FOUND',
  PLATFORM_NOT_FOUND = 'PLATFORM_NOT_FOUND',
  USER_NOT_FOUND = 'USER_NOT_FOUND',
  DUPLICATE_ORDER = 'DUPLICATE_ORDER',
  INSUFFICIENT_BALANCE = 'INSUFFICIENT_BALANCE',

  // Server errors
  INTERNAL_SERVER_ERROR = 'INTERNAL_SERVER_ERROR',
  SERVICE_UNAVAILABLE = 'SERVICE_UNAVAILABLE'
}

/**
 * Get user-friendly error message
 */
export function getErrorMessage(errorCode: string | undefined): string {
  const messages: Record<string, string> = {
    [ErrorCode.INVALID_FILE]: 'File không hợp lệ. Vui lòng chọn file CSV.',
    [ErrorCode.INVALID_FILE_TYPE]: 'Chỉ chấp nhận file CSV.',
    [ErrorCode.FILE_READ_ERROR]: 'Không thể đọc file. Vui lòng thử lại.',
    [ErrorCode.FILE_TOO_LARGE]: 'File quá lớn. Kích thước tối đa là 10MB.',

    [ErrorCode.UNAUTHORIZED]: 'Vui lòng đăng nhập lại.',
    [ErrorCode.INVALID_TOKEN]: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    [ErrorCode.FORBIDDEN]: 'Bạn không có quyền thực hiện chức năng này.',

    [ErrorCode.NOT_FOUND]: 'Không tìm thấy dữ liệu.',
    [ErrorCode.PLATFORM_NOT_FOUND]: 'Nền tảng không tồn tại. Vui lòng liên hệ admin.',
    [ErrorCode.USER_NOT_FOUND]: 'Không tìm thấy người dùng.',
    [ErrorCode.DUPLICATE_ORDER]: 'Đơn hàng đã tồn tại trong hệ thống.',
    [ErrorCode.INSUFFICIENT_BALANCE]: 'Số dư không đủ.',

    [ErrorCode.INTERNAL_SERVER_ERROR]: 'Đã xảy ra lỗi. Vui lòng thử lại sau.',
    [ErrorCode.SERVICE_UNAVAILABLE]: 'Dịch vụ tạm thời không khả dụng.'
  };

  return messages[errorCode || ''] || 'Đã xảy ra lỗi không xác định.';
}
