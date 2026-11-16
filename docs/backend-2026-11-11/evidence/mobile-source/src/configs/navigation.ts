import type { NavigatorScreenParams } from '@react-navigation/native';

export enum RouteName {
  KeoMain = 'KeoMain',
  OnlineRooms = 'OnlineRooms',
  OnlineRoomDetail = 'OnlineRoomDetail',
  KeoCapture = 'KeoCapture',
  KeoPhotoPreview = 'KeoPhotoPreview',
  KeoPhotoDetail = 'KeoPhotoDetail',
  KeoHome = 'KeoHome',
  KeoRoomDetail = 'KeoRoomDetail',
}
export type AppStackParamList = {
  [RouteName.KeoMain]: NavigatorScreenParams<KeoTabParamList> | undefined;
  [RouteName.KeoCapture]: { roomId?: string };
  [RouteName.KeoPhotoPreview]: { uri: string; width: number; height: number; roomId?: string; capturedForScope: string };
  [RouteName.KeoPhotoDetail]: { photoId: string };
  [RouteName.KeoHome]: undefined;
  [RouteName.KeoRoomDetail]: { roomId: string };
};
export type RoomStackParamList = {
  [RouteName.OnlineRooms]: undefined;
  [RouteName.OnlineRoomDetail]: { roomId: string };
};
export type KeoTabParamList = {
  Home: undefined;
  Rooms: NavigatorScreenParams<RoomStackParamList> | undefined;
  Messages: undefined;
  Time: undefined;
};
