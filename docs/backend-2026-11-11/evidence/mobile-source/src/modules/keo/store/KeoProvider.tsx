import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { createMMKV } from 'react-native-mmkv';
import { RoomState } from '../types';
import {
  SavedRoom,
  createDemoRoom,
  createLocalRoom,
  decodeRooms,
  encodeRooms,
} from '../utils/roomStorage';

const storage = createMMKV({ id: 'let-play-rooms' });
const KEY = 'rooms.v1';
type RoomUpdate = (previous: RoomState) => RoomState;
type Store = {
  rooms: SavedRoom[];
  storageError: string;
  createRoom: (name: string, displayName: string, friends: string[]) => string;
  openDemo: () => string;
  updateRoom: (id: string, update: RoomUpdate) => boolean;
  archiveRoom: (id: string) => void;
  deleteRoom: (id: string) => void;
};
const StoreContext = createContext<Store | null>(null);
export function KeoProvider({ children }: React.PropsWithChildren) {
  const [initial] = useState(() => {
    try {
      return { rooms: decodeRooms(storage.getString(KEY)), error: '' };
    } catch {
      return {
        rooms: [] as SavedRoom[],
        error: 'Không đọc được dữ liệu cũ trên máy. Bạn có thể tạo phòng mới.',
      };
    }
  });
  const [storageError, setStorageError] = useState(initial.error);
  const [rooms, setRooms] = useState<SavedRoom[]>(initial.rooms);
  const latest = useRef(rooms);
  // Persist before publishing the new UI state; a failed write is surfaced to the user.
  const commit = useCallback((update: (previous: SavedRoom[]) => SavedRoom[]) => {
    const next = update(latest.current);
    try {
      storage.set(KEY, encodeRooms(next));
      setStorageError('');
    } catch {
      setStorageError('Chưa lưu được thay đổi trên máy. Hãy giữ app mở và thử lại.');
      return false;
    }
    latest.current = next;
    setRooms(next);
    return true;
  }, []);
  const createRoom = (name: string, displayName: string, friends: string[]) => {
    let code: string;
    do {
      code = Math.random().toString(36).slice(2, 8).toUpperCase().padEnd(6, '0');
    } while (latest.current.some((room) => room.code === code));
    const room = createLocalRoom(name, displayName, friends, code);
    if (!commit((previous) => [room, ...previous])) throw new Error('Chưa lưu được phòng trên thiết bị. Hãy thử lại.');
    return room.id;
  };
  const openDemo = () => {
    if (!latest.current.some((room) => room.id === 'demo'))
      if (!commit((previous) => [createDemoRoom(), ...previous])) throw new Error('Chưa lưu được phòng mẫu.');
    return 'demo';
  };
  const updateRoom = useCallback(
    (id: string, update: RoomUpdate) =>
      commit((previous) =>
        previous.map((room) =>
          room.id === id ? { ...room, state: update(room.state), updatedAt: Date.now() } : room,
        ),
      ),
    [commit],
  );
  const archiveRoom = (id: string) =>
    commit((previous) =>
      previous.map((room) => (room.id === id ? { ...room, archived: !room.archived } : room)),
    );
  const deleteRoom = (id: string) =>
    commit((previous) => previous.filter((room) => room.id !== id));
  return (
    <StoreContext.Provider
      value={{ rooms, storageError, createRoom, openDemo, updateRoom, archiveRoom, deleteRoom }}
    >
      {children}
    </StoreContext.Provider>
  );
}
export function useKeoStore() {
  const value = useContext(StoreContext);
  if (!value) throw new Error('KeoProvider is missing');
  return value;
}
const RoomContext = createContext<string | null>(null);
export function RoomProvider({ roomId, children }: React.PropsWithChildren<{ roomId: string }>) {
  return <RoomContext.Provider value={roomId}>{children}</RoomContext.Provider>;
}
export function useRoomSession() {
  const id = useContext(RoomContext);
  const { rooms, updateRoom } = useKeoStore();
  const room = rooms.find((item) => item.id === id);
  const setState = useCallback(
    (update: RoomUpdate) => {
      return id ? updateRoom(id, update) : false;
    },
    [id, updateRoom],
  );
  if (!room) throw new Error('Không tìm thấy phòng trên thiết bị này.');
  return { room, members: room.members, currentUserIndex: room.state.currentUserIndex, setState };
}
