/**
 * Mock API Service for CashBee Frontend Development
 *
 * This file provides a complete mock implementation of the CashBee API
 * for Phase 5 & 6 (Affiliate Tracking & Import).
 *
 * Usage:
 * 1. Copy this file to your frontend project (e.g., src/services/mock-api.ts)
 * 2. Use MockApiService instead of real API during development
 * 3. Switch to real API when backend is ready
 *
 * @author CashBee Team
 * @version 1.0.0
 */

// ==================== TYPE DEFINITIONS ====================

interface CreateTrackingLinkRequest {
  shopeeUrl: string;
  platformCode?: string;
  userId: number;
}

interface TrackingLinkResponse {
  clickId: number;
  trackingUrl: string;
  trackingCode: string;
  originalUrl: string;
  productName: string | null;
  shopId: string;
  itemId: string;
  platformName: string;
  platformCode: string;
  estimatedCashbackRate: number;
  createdAt: string;
  message: string;
}

interface ImportOrdersRequest {
  file: File;
  platformCode?: string;
  importedBy: number;
  skipDuplicates?: boolean;
  autoMatch?: boolean;
}

interface ImportOrdersResponse {
  batchId: number;
  platformName: string;
  platformCode: string;
  fileName: string;
  status: ImportStatus;
  totalRows: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  matchedCount: number;
  successRate: number;
  errorMessage: string | null;
  errors: ImportErrorDetail[];
  startedAt: string;
  completedAt: string;
  durationSeconds: number;
  importedBy: number;
}

type ImportStatus = 'PROCESSING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';

interface ImportErrorDetail {
  rowNumber: number;
  orderId: string | null;
  error: string;
  rawData: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}

interface ApiError {
  success: false;
  errorCode: string;
  message: string;
  data: null;
  timestamp: string;
}

// ==================== MOCK DATA STORAGE ====================

class MockDatabase {
  private clicks: Map<number, any> = new Map();
  private batches: Map<number, any> = new Map();
  private clickIdCounter = 100;
  private batchIdCounter = 1;

  saveClick(click: any): number {
    const clickId = this.clickIdCounter++;
    this.clicks.set(clickId, { ...click, id: clickId });
    return clickId;
  }

  getClick(clickId: number): any | null {
    return this.clicks.get(clickId) || null;
  }

  markClickAsClicked(clickId: number): void {
    const click = this.clicks.get(clickId);
    if (click) {
      click.clickedAt = new Date().toISOString();
      click.status = 'CLICKED';
    }
  }

  saveBatch(batch: any): number {
    const batchId = this.batchIdCounter++;
    this.batches.set(batchId, { ...batch, id: batchId });
    return batchId;
  }

  getBatch(batchId: number): any | null {
    return this.batches.get(batchId) || null;
  }

  getAllBatches(): any[] {
    return Array.from(this.batches.values());
  }
}

const mockDb = new MockDatabase();

// ==================== MOCK API SERVICE ====================

export class MockApiService {
  private readonly MOCK_DELAY_SHORT = 800; // 800ms for quick operations
  private readonly MOCK_DELAY_LONG = 3000; // 3s for import operations
  private readonly BASE_URL = 'http://localhost:8080/api';

  /**
   * Create affiliate tracking link
   *
   * @param request - Request with Shopee URL and user ID
   * @returns Tracking link response
   * @throws Error if URL is invalid
   */
  async createTrackingLink(
    request: CreateTrackingLinkRequest
  ): Promise<ApiResponse<TrackingLinkResponse>> {
    console.log('[MOCK API] Creating tracking link:', request);

    // Simulate network delay
    await this.delay(this.MOCK_DELAY_SHORT);

    // Validate URL
    if (!this.isValidShopeeUrl(request.shopeeUrl)) {
      throw this.createError('INVALID_URL', 'Invalid Shopee URL format');
    }

    // Parse URL
    const parsed = this.parseShopeeUrl(request.shopeeUrl);

    // Generate tracking code
    const clickId = mockDb.saveClick({
      userId: request.userId,
      originalUrl: request.shopeeUrl,
      createdAt: new Date().toISOString(),
      status: 'CREATED'
    });

    const timestamp = new Date().toISOString().replace(/[-:T.Z]/g, '').slice(0, 14);
    const trackingCode = `CB${request.userId}_${clickId}_${timestamp}`;

    // Build tracking URL
    const trackingUrl = this.buildTrackingUrl(parsed.itemId, trackingCode);

    const response: TrackingLinkResponse = {
      clickId,
      trackingUrl,
      trackingCode,
      originalUrl: request.shopeeUrl,
      productName: this.generateMockProductName(),
      shopId: parsed.shopId || 'unknown',
      itemId: parsed.itemId,
      platformName: 'Shopee',
      platformCode: 'shopee',
      estimatedCashbackRate: this.randomCashbackRate(),
      createdAt: new Date().toISOString(),
      message: 'Click this link to shop on Shopee and earn cashback!'
    };

    console.log('[MOCK API] Tracking link created:', response);

    return {
      success: true,
      message: 'Tracking link created successfully',
      data: response,
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Handle click redirect (mock)
   *
   * In real implementation, this would perform HTTP 302 redirect.
   * In mock, we just return the tracking URL.
   *
   * @param clickId - Click ID
   * @returns Tracking URL
   * @throws Error if click not found
   */
  async handleClickRedirect(clickId: number): Promise<string> {
    console.log('[MOCK API] Handling click redirect:', clickId);

    await this.delay(300);

    const click = mockDb.getClick(clickId);
    if (!click) {
      throw this.createError('CLICK_NOT_FOUND', 'Tracking link not found or expired');
    }

    // Mark as clicked
    mockDb.markClickAsClicked(clickId);

    console.log('[MOCK API] Redirecting to:', click.trackingUrl);

    // In real implementation, browser would redirect
    // In mock, we just return the URL
    return click.trackingUrl || this.buildTrackingUrl('123456', 'CB1_100_20251101');
  }

  /**
   * Import orders from CSV file
   *
   * @param request - Import request with file and options
   * @returns Import statistics
   * @throws Error if file is invalid
   */
  async importOrders(
    request: ImportOrdersRequest
  ): Promise<ApiResponse<ImportOrdersResponse>> {
    console.log('[MOCK API] Importing orders:', request.file.name);

    // Validate file
    if (!request.file.name.toLowerCase().endsWith('.csv')) {
      throw this.createError('INVALID_FILE_TYPE', 'Only CSV files are supported');
    }

    if (request.file.size === 0) {
      throw this.createError('INVALID_FILE', 'CSV file is empty');
    }

    // Simulate long processing
    await this.delay(this.MOCK_DELAY_LONG);

    // Generate realistic statistics
    const totalRows = Math.floor(Math.random() * 1000) + 1500; // 1500-2500 rows
    const successRate = 0.85 + Math.random() * 0.13; // 85-98%
    const successCount = Math.floor(totalRows * successRate);
    const failedCount = Math.floor((totalRows - successCount) * 0.4);
    const skippedCount = totalRows - successCount - failedCount;
    const matchedCount = Math.floor(successCount * 0.9); // 90% matched

    const startTime = new Date(Date.now() - this.MOCK_DELAY_LONG);
    const endTime = new Date();

    const batch = {
      platformId: 1,
      fileName: request.file.name,
      totalRows,
      successCount,
      failedCount,
      skippedCount,
      status: this.determineImportStatus(successRate),
      createdAt: startTime.toISOString(),
      completedAt: endTime.toISOString(),
      importedBy: request.importedBy
    };

    const batchId = mockDb.saveBatch(batch);

    const response: ImportOrdersResponse = {
      batchId,
      platformName: 'Shopee',
      platformCode: 'shopee',
      fileName: request.file.name,
      status: batch.status,
      totalRows,
      successCount,
      failedCount,
      skippedCount,
      matchedCount,
      successRate: successRate * 100,
      errorMessage: null,
      errors: this.generateMockErrors(failedCount),
      startedAt: startTime.toISOString(),
      completedAt: endTime.toISOString(),
      durationSeconds: Math.floor((endTime.getTime() - startTime.getTime()) / 1000),
      importedBy: request.importedBy
    };

    console.log('[MOCK API] Import completed:', response);

    return {
      success: true,
      message: this.buildImportMessage(response),
      data: response,
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Get import batch details
   *
   * @param batchId - Batch ID
   * @returns Batch details
   * @throws Error if batch not found
   */
  async getImportBatch(batchId: number): Promise<ApiResponse<any>> {
    console.log('[MOCK API] Getting batch:', batchId);

    await this.delay(300);

    const batch = mockDb.getBatch(batchId);
    if (!batch) {
      throw this.createError('BATCH_NOT_FOUND', `Import batch not found: ${batchId}`);
    }

    return {
      success: true,
      message: 'Batch details retrieved',
      data: batch,
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Get recent import batches
   *
   * @param limit - Number of batches to return
   * @returns List of recent batches
   */
  async getRecentBatches(limit: number = 10): Promise<ApiResponse<any[]>> {
    console.log('[MOCK API] Getting recent batches, limit:', limit);

    await this.delay(300);

    const batches = mockDb.getAllBatches()
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, limit);

    return {
      success: true,
      message: `Retrieved ${batches.length} batches`,
      data: batches,
      timestamp: new Date().toISOString()
    };
  }

  // ==================== HELPER METHODS ====================

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private isValidShopeeUrl(url: string): boolean {
    return url.includes('shopee.vn') && (
      url.includes('/product/') ||
      url.includes('-i.') ||
      url.includes('/universal-link/')
    );
  }

  private parseShopeeUrl(url: string): { shopId: string | null; itemId: string } {
    // Format 1: /product/{shop_id}/{item_id}
    const format1 = url.match(/\/product\/(\d+)\/(\d+)/);
    if (format1) {
      return { shopId: format1[1], itemId: format1[2] };
    }

    // Format 2 & 3: -i.{shop_id}.{item_id}
    const format2 = url.match(/-i\.(\d+)\.(\d+)/);
    if (format2) {
      return { shopId: format2[1], itemId: format2[2] };
    }

    // Format 4: /universal-link/{item_id}
    const format4 = url.match(/\/universal-link\/(\d+)/);
    if (format4) {
      return { shopId: null, itemId: format4[1] };
    }

    // Fallback: generate random IDs
    return {
      shopId: Math.floor(Math.random() * 100000000).toString(),
      itemId: Math.floor(Math.random() * 10000000000).toString()
    };
  }

  private buildTrackingUrl(itemId: string, trackingCode: string): string {
    return `https://shopee.vn/universal-link/${itemId}?af_siteid=0&pid=cashbee_vn_123456&af_sub1=${encodeURIComponent(trackingCode)}`;
  }

  private generateMockProductName(): string {
    const products = [
      'Dây Nhảy Thể Dục, Dây Nhảy Thể Lực Tập Thể Dục',
      'Áo len cộc vặn thừng TAG SẮT chất mềm mịn',
      'Bột Protein Thực Vật Ultimate Plant Bổ Sung Đạm',
      'Set 2 bát gấu - BÁT LỢN HEO kèm 2 thìa',
      'Set Trà Sữa Tự Pha Trân Châu Đường Đen',
      'Đầu ghép máy thần thánh Plastic 7 cái/vỉ',
      'Túi đeo vai thời trang dáng thuyền phong cách',
      'Nước thanh khiết pha sữa AOI an toàn cho bé',
      'Khăn Giấy Rút Treo Tường Bông Sen Vàng',
      'Kem Nền Che Khuyết Điểm Maycheer Cover Face'
    ];

    return products[Math.floor(Math.random() * products.length)];
  }

  private randomCashbackRate(): number {
    const rates = [3.00, 4.00, 5.00, 6.00, 7.00, 8.00];
    return rates[Math.floor(Math.random() * rates.length)];
  }

  private determineImportStatus(successRate: number): ImportStatus {
    if (successRate >= 0.98) return 'COMPLETED';
    if (successRate >= 0.70) return 'PARTIAL';
    return 'FAILED';
  }

  private generateMockErrors(count: number): ImportErrorDetail[] {
    const errors: ImportErrorDetail[] = [];
    const errorTypes = [
      'Duplicate order ID',
      'Invalid commission amount format',
      'Missing order ID',
      'No tracking code in Sub_id1',
      'Invalid date format',
      'Commission amount is negative',
      'Order status is invalid',
      'Failed to parse row'
    ];

    const maxErrors = Math.min(count, 20); // Max 20 errors in response

    for (let i = 0; i < maxErrors; i++) {
      const hasOrderId = Math.random() > 0.3;
      errors.push({
        rowNumber: Math.floor(Math.random() * 2000) + 1,
        orderId: hasOrderId ? this.generateMockOrderId() : null,
        error: errorTypes[Math.floor(Math.random() * errorTypes.length)],
        rawData: this.generateMockCsvRow()
      });
    }

    return errors.sort((a, b) => a.rowNumber - b.rowNumber);
  }

  private generateMockOrderId(): string {
    const prefix = '25102';
    const suffix = Math.random().toString(36).substring(2, 11).toUpperCase();
    return prefix + suffix;
  }

  private generateMockCsvRow(): string {
    return `${this.generateMockOrderId()},Đang chờ xử lý,215453681217374,2025-10-29 23:14:53,...`;
  }

  private buildImportMessage(response: ImportOrdersResponse): string {
    if (response.status === 'COMPLETED') {
      return `Successfully imported ${response.successCount} orders. ${response.matchedCount} orders matched with clicks.`;
    } else if (response.status === 'PARTIAL') {
      return `Partially imported ${response.successCount} orders (${response.failedCount} failed, ${response.skippedCount} skipped). ${response.matchedCount} orders matched.`;
    } else {
      return `Import failed. ${response.failedCount} orders failed to import.`;
    }
  }

  private createError(code: string, message: string): ApiError {
    return {
      success: false,
      errorCode: code,
      message,
      data: null,
      timestamp: new Date().toISOString()
    };
  }
}

// ==================== SINGLETON INSTANCE ====================

export const mockApiService = new MockApiService();

// ==================== USAGE EXAMPLES ====================

/**
 * Example 1: Create Tracking Link
 */
async function exampleCreateLink() {
  try {
    const response = await mockApiService.createTrackingLink({
      shopeeUrl: 'https://shopee.vn/product/47305935/20317610036',
      platformCode: 'shopee',
      userId: 1
    });

    console.log('Tracking URL:', response.data.trackingUrl);
    console.log('Cashback Rate:', response.data.estimatedCashbackRate + '%');
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

/**
 * Example 2: Handle Click Redirect
 */
async function exampleHandleClick() {
  try {
    const trackingUrl = await mockApiService.handleClickRedirect(100);
    console.log('Redirecting to:', trackingUrl);

    // In real app: window.location.href = trackingUrl;
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

/**
 * Example 3: Import Orders
 */
async function exampleImportOrders() {
  // Create a mock file
  const csvContent = 'ID đơn hàng,Trạng thái đặt hàng,...\n251030D6GR3SET,Đang chờ xử lý,...';
  const blob = new Blob([csvContent], { type: 'text/csv' });
  const file = new File([blob], 'orders.csv', { type: 'text/csv' });

  try {
    const response = await mockApiService.importOrders({
      file,
      platformCode: 'shopee',
      importedBy: 5,
      skipDuplicates: true,
      autoMatch: true
    });

    console.log('Import Status:', response.data.status);
    console.log('Success Count:', response.data.successCount);
    console.log('Failed Count:', response.data.failedCount);
    console.log('Matched Count:', response.data.matchedCount);
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

/**
 * Example 4: Get Import Batch
 */
async function exampleGetBatch() {
  try {
    const response = await mockApiService.getImportBatch(1);
    console.log('Batch:', response.data);
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

/**
 * Example 5: Get Recent Batches
 */
async function exampleGetRecentBatches() {
  try {
    const response = await mockApiService.getRecentBatches(10);
    console.log('Recent Batches:', response.data);
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

// Export examples for testing
export const examples = {
  exampleCreateLink,
  exampleHandleClick,
  exampleImportOrders,
  exampleGetBatch,
  exampleGetRecentBatches
};

// ==================== REACT HOOK EXAMPLE ====================

/**
 * React Hook for using Mock API
 *
 * Usage in React component:
 *
 * ```typescript
 * function MyComponent() {
 *   const { createLink, loading, error } = useMockApi();
 *
 *   const handleSubmit = async (url: string) => {
 *     const result = await createLink(url, 1);
 *     console.log('Tracking URL:', result.trackingUrl);
 *   };
 *
 *   return (
 *     <div>
 *       {loading && <p>Loading...</p>}
 *       {error && <p>Error: {error}</p>}
 *     </div>
 *   );
 * }
 * ```
 */
export function useMockApi() {
  // In real implementation, use React.useState and React.useCallback
  // This is a simplified version

  const createLink = async (shopeeUrl: string, userId: number) => {
    const response = await mockApiService.createTrackingLink({
      shopeeUrl,
      userId,
      platformCode: 'shopee'
    });
    return response.data;
  };

  const importOrders = async (file: File, adminUserId: number) => {
    const response = await mockApiService.importOrders({
      file,
      importedBy: adminUserId,
      platformCode: 'shopee',
      skipDuplicates: true,
      autoMatch: true
    });
    return response.data;
  };

  return {
    createLink,
    importOrders,
    handleClickRedirect: mockApiService.handleClickRedirect.bind(mockApiService),
    getImportBatch: mockApiService.getImportBatch.bind(mockApiService),
    getRecentBatches: mockApiService.getRecentBatches.bind(mockApiService)
  };
}
