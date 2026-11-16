export type Phase = 'ideas' | 'vote' | 'order' | 'plan';

export type Role = 'host' | 'member';

export type IdeaCategory = 'food' | 'book' | 'camera' | 'coffee' | 'pin';

export interface Idea {
  id: number;
  title: string;
  note: string;
  cost: number | null; // VND per person, null if unknown, 0 if free
  min: number | null; // estimated duration in minutes
  type: IdeaCategory;
  mine?: boolean; // created by current user
  base: [number, number, number]; // [very_want, can_join, next_time] simulated votes from friends
  authorName?: string;
  ownerIndex?: number; // Chỉ dùng cho sổ/phòng local; không là danh tính online.
}

export type VoteValue = 2 | 1 | 0; // 2: Rất muốn, 1: Đi cùng được, 0: Để dịp khác

export interface Expense {
  id: number;
  title: string;
  amount: number; // integer VND (1 -> 100,000,000)
  payer: number; // membership index in MEMBERS
  participants: number[]; // list of membership indices
  shares: number[]; // exact amount per membership index, sum == amount
  method: 'equal' | 'custom';
  createdAt?: string;
}

export type TransferStatus = 'pending' | 'confirmed' | 'rejected';

export interface Transfer {
  id: number;
  from: number; // membership index who owes
  to: number; // membership index who receives
  amount: number; // VND
  status: TransferStatus;
  createdAt?: string;
}

export interface Debt {
  from: number;
  to: number;
  amount: number;
}

export interface RoomState {
  roomName: string;
  phase: Phase;
  role: Role;
  currentUserIndex: number;
  ideas: Idea[];
  votes: Record<number, VoteValue>;
  endorsed: number[];
  selected: number[];
  order: number[];
  orderSent: boolean;
  submitted: boolean;
  completed: number[];
  notes: string[];
  expenses: Expense[];
  transfers: Transfer[];
  moneyView: boolean;
  startTime?: string;
  lifecycle?: 'active' | 'ended';
  endedAt?: number;
  planVersion?: number;
  planHistory?: PlanSnapshot[];
  draftOrder?: number[];
  draftStartTime?: string;
  planReason?: string;
  votesByMember?: Record<number, Record<number, VoteValue>>;
  orderBallots?: Record<number, number[]>;
}

export interface PlanSnapshot {
  version: number;
  order: number[];
  startTime: string;
  completed: number[];
  ideas: Idea[];
  reason: string;
  savedAt: number;
}

export type ActionResult = { success: boolean; message?: string };
