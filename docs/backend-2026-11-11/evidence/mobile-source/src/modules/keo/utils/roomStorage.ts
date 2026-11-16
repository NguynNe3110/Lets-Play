import { z } from 'zod';
import { getInitialRoomState } from '../data/mockData';
import { RoomState } from '../types';

export type SavedRoom = {
  id: string;
  code: string;
  members: string[];
  displayName: string;
  isDemo: boolean;
  archived: boolean;
  createdAt: number;
  updatedAt: number;
  state: RoomState;
};
const indices = z.array(z.number().int().nonnegative());
const money = z.number().int().min(0).max(100000000);
const stateSchema = z.object({
  roomName: z.string().min(1),
  phase: z.enum(['ideas', 'vote', 'order', 'plan']),
  role: z.enum(['host', 'member']),
  currentUserIndex: z.number().int().nonnegative(),
  ideas: z.array(
    z.object({
      id: z.number(),
      title: z.string(),
      note: z.string(),
      cost: money.nullable(),
      min: z.number().nonnegative().nullable(),
      type: z.enum(['food', 'coffee', 'book', 'camera', 'pin']),
      mine: z.boolean().optional(),
      ownerIndex: z.number().int().nonnegative().optional(),
      base: z.tuple([z.number().nonnegative(), z.number().nonnegative(), z.number().nonnegative()]),
    }),
  ),
  votes: z.record(
    z
      .enum(['0', '1', '2'])
      .transform(Number)
      .or(z.union([z.literal(0), z.literal(1), z.literal(2)])),
  ),
  endorsed: indices,
  selected: indices,
  order: indices,
  orderSent: z.boolean(),
  submitted: z.boolean(),
  completed: indices,
  notes: z.array(z.string()),
  moneyView: z.boolean(),
  startTime: z
    .string()
    .regex(/^([01]\d|2[0-3]):[0-5]\d$/)
    .default('18:00'),
  expenses: z.array(
    z.object({
      id: z.number(),
      title: z.string(),
      amount: money.positive(),
      payer: z.number().int().nonnegative(),
      participants: indices,
      shares: z.array(money),
      method: z.enum(['equal', 'custom']),
      createdAt: z.string().optional(),
    }),
  ),
  transfers: z.array(
    z.object({
      id: z.number(),
      from: z.number().int().nonnegative(),
      to: z.number().int().nonnegative(),
      amount: money.positive(),
      status: z.enum(['pending', 'confirmed', 'rejected']),
      createdAt: z.string().optional(),
    }),
  ),
  lifecycle: z.enum(['active', 'ended']).optional(),
  endedAt: z.number().optional(),
  planVersion: z.number().int().nonnegative().optional(),
  draftOrder: indices.optional(),
  draftStartTime: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/).optional(),
  planReason: z.string().optional(),
  votesByMember: z.record(z.record(z.union([z.literal(0), z.literal(1), z.literal(2)]))).optional(),
  orderBallots: z.record(indices).optional(),
  planHistory: z.array(z.object({ version: z.number().int().positive(), order: indices, startTime: z.string(), completed: indices, ideas: z.array(z.object({ id: z.number(), title: z.string(), note: z.string(), cost: money.nullable(), min: z.number().nonnegative().nullable(), type: z.enum(['food', 'coffee', 'book', 'camera', 'pin']), mine: z.boolean().optional(), ownerIndex: z.number().int().nonnegative().optional(), base: z.tuple([z.number().nonnegative(), z.number().nonnegative(), z.number().nonnegative()]) })), reason: z.string(), savedAt: z.number() })).optional(),
});
const schema = z.object({
  version: z.literal(1),
  rooms: z.array(
    z.object({
      id: z.string(),
      code: z.string(),
      members: z.array(z.string()).min(1).max(30),
      displayName: z.string(),
      isDemo: z.boolean(),
      archived: z.boolean(),
      createdAt: z.number(),
      updatedAt: z.number(),
      state: stateSchema,
    }),
  ),
});

export function decodeRooms(raw: string | undefined): SavedRoom[] {
  if (!raw) return [];
  const parsed = schema.parse(JSON.parse(raw));
  return parsed.rooms as SavedRoom[];
}
export function encodeRooms(rooms: SavedRoom[]) {
  return JSON.stringify({ version: 1, rooms });
}
export function createLocalRoom(
  name: string,
  displayName: string,
  friends: string[],
  code: string,
): SavedRoom {
  if (!name.trim() || name.trim().length > 100) throw new Error('Tên kèo cần từ 1–100 ký tự.');
  if (!displayName.trim() || displayName.trim().length > 60) throw new Error('Tên của bạn cần từ 1–60 ký tự.');
  if (friends.filter(n => n.trim()).length > 29 || friends.some(n => n.trim().length > 60)) throw new Error('Phòng local có tối đa 30 người, tên không quá 60 ký tự.');
  const now = Date.now();
  return {
    id: `room-${code}`,
    code,
    members: [displayName.trim(), ...friends.map((n) => n.trim()).filter(Boolean)],
    displayName: displayName.trim(),
    isDemo: false,
    archived: false,
    createdAt: now,
    updatedAt: now,
    state: {
      roomName: name.trim(),
      phase: 'ideas',
      role: 'host',
      currentUserIndex: 0,
      ideas: [],
      votes: {},
      endorsed: [],
      selected: [],
      order: [],
      orderSent: false,
      submitted: false,
      completed: [],
      notes: [],
      expenses: [],
      transfers: [],
      moneyView: false,
      startTime: '18:00',
    },
  };
}
export function createDemoRoom(): SavedRoom {
  const room = createLocalRoom(
    'Tối thứ Bảy của tụi mình',
    'An',
    ['Minh', 'Linh', 'Hà', 'Nam', 'Vy'],
    'KEO726',
  );
  return { ...room, id: 'demo', isDemo: true, state: getInitialRoomState() };
}
