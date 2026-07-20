// These interfaces describe the "shape" of the JSON data coming from (or sent to)
// the Spring Boot backend. They give us autocomplete and type-checking in Angular.

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  name: string;
}

export interface UserSettingsResponse {
  configured: boolean;
  monthlySalary: number | null;
  salaryPaymentDay: number | null;
  monthlyWorkingHours: number | null;
  currency: string;
  salaryWalletId: number | null;
}

export interface UserSettingsRequest {
  monthlySalary: number;
  salaryPaymentDay: number;
  monthlyWorkingHours: number;
  currency: string;
  salaryWalletId: number | null;
}

export interface GardenResponse {
  level: number;
  stageName: string;
  stageEmoji: string;
  growthPercentage: number;
  statusMessage: string;
}

export interface TransactionResponse {
  id?: number;
  description: string;
  amount: number;
  date: string;
  categoryName?: string;
  categoryId?: number;
  timeCostHours: number | null;
  timeCostMessage: string | null;
}

export interface BadgeDto {
  name: string;
  emoji: string;
  description: string;
  earned: boolean;
}

export interface StreakResponse {
  currentStreak: number;
  longestStreak: number;
  isNewRecord: boolean;
  streakMessage: string;
  badges: BadgeDto[];
}

export interface RecurringTransactionResponse {
  id: number;
  description: string;
  amount: number;
  dayOfMonth: number;
  active: boolean;
  categoryId: number | null;
  categoryName: string | null;
}

export interface RecurringTransactionRequest {
  description: string;
  amount: number;
  dayOfMonth: number;
  categoryId: number;
  active: boolean;
}

export interface WalletResponse {
  id: number;
  name: string;
  balance: number;
}

export interface WalletRequest {
  name: string;
}

export interface Category {
  id?: number;
  name: string;
  monthlyLimit: number;
  walletId: number | null;
  walletName?: string;
}

// What we POST to create a transaction (backend expects a categoryId, not a full Category)
export interface NewTransactionRequest {
  description: string;
  amount: number;
  date: string;
  categoryId: number;
}

export interface Dream {
  id?: number;
  name: string;
  targetAmount: number;
  savedAmount?: number;
}

export interface PaceWarning {
  categoryName: string;
  remainingBudget: number;
  daysLeftAtCurrentPace: number;
  message: string;
}

export interface DreamStatus {
  dreamName: string;
  targetAmount: number;
  savedAmount: number;
  progressPercent: number;
  message: string;
}

export interface MemoryLaneEntry {
  monthLabel: string;
  totalSpent: number;
  message: string;
}

export type PlanTier = 'FREE' | 'PLUS' | 'PREMIUM';
export type BillingInterval = 'MONTHLY' | 'ANNUAL';

export interface SubscriptionStatusResponse {
  billingStatus: 'TRIAL' | 'ACTIVE' | 'EXPIRED';
  currentTier: PlanTier;
  trialDaysRemaining: number;
}

export interface CheckoutRequest {
  tier: 'PLUS' | 'PREMIUM';
  interval: BillingInterval;
}

export interface CheckoutSessionResponse {
  checkoutUrl: string;
}

export interface CsvImportResponse {
  imported: number;
  skipped: number;
  errors: string[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardResponse {
  nickname: string;
  dailyTitleMessage: string;
  thisMonthTotal: number;
  lastMonthTotal: number;
  twinComparisonMessage: string;
  wallets: WalletResponse[];
  currency: string;
  emailVerified: boolean;
  paceWarnings: PaceWarning[];
  dreamStatuses: DreamStatus[];
  memoryLane: MemoryLaneEntry[];
  periodicReportMessage: string | null;
  currentTier: PlanTier;
  plusLocked: boolean;
  premiumLocked: boolean;
  trialDaysRemaining: number;
  upgradeMessage: string | null;
  premiumUpsellMessage: string | null;
  salaryInsights: string[];
  timeCostSummaryMessage: string | null;
  gardenStatus: GardenResponse | null;
  streakStatus: StreakResponse | null;
}
