import React from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createNativeStackNavigator, NativeStackScreenProps } from '@react-navigation/native-stack';
import { createBottomTabNavigator, BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppStackParamList, KeoTabParamList, RoomStackParamList, RouteName } from 'configs/navigation';
import { Text } from 'components';
import { IconName, PlayIcon } from 'design/PlayIcon';
import { KeoHomeScreen, KeoRoomDetailScreen } from 'modules/keo';
import { RoomProvider, useKeoStore } from 'modules/keo/store/KeoProvider';
import { theme } from 'design/theme';
import { navigationRef } from './navigationUtilities';
import { OnlineProvider } from 'modules/keo/store/OnlineProvider';
import { PhotoProvider } from 'modules/keo/store/PhotoProvider';
import { OnlineRoomsScreen } from 'modules/keo/screens/OnlineRoomsScreen';
import { OnlineRoomScreen } from 'modules/keo/screens/OnlineRoomScreen';
import { CameraHomeScreen } from 'modules/keo/screens/CameraHomeScreen';
import { MemoriesScreen, PhotoDetailScreen, PhotoPreviewScreen } from 'modules/keo/screens/PhotoScreens';
import { MessagesScreen } from 'modules/keo/screens/MessagesScreen';

export type AppStackScreenProps<T extends keyof AppStackParamList> = NativeStackScreenProps<AppStackParamList, T>;
const Stack = createNativeStackNavigator<AppStackParamList>();
const RoomsStack = createNativeStackNavigator<RoomStackParamList>();
const Tabs = createBottomTabNavigator<KeoTabParamList>();
function RoomNavigation() {
  return <RoomsStack.Navigator screenOptions={{ headerShown: false }}><RoomsStack.Screen name={RouteName.OnlineRooms} component={OnlineRoomsScreen} /><RoomsStack.Screen name={RouteName.OnlineRoomDetail} component={OnlineRoomScreen} getId={({ params }) => params.roomId} /></RoomsStack.Navigator>;
}
const tabItems: Record<keyof KeoTabParamList, { label: string; icon: IconName }> = {
  Home: { label: 'Home', icon: 'home' }, Rooms: { label: 'Kèo', icon: 'calendar' }, Messages: { label: 'Tin nhắn', icon: 'chat' }, Time: { label: 'Thời gian', icon: 'clock' },
};
function FloatingTabs({ state, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  return <View style={[styles.tabs, { bottom: Math.max(insets.bottom, 12) }]}>{state.routes.map((route, index) => {
    const selected = state.index === index, item = tabItems[route.name as keyof KeoTabParamList];
    return <Pressable key={route.key} accessibilityRole="tab" accessibilityLabel={item.label} accessibilityState={{ selected }} style={({ pressed }) => [styles.tab, selected && styles.activeTab, pressed && { opacity: 0.65 }]} onPress={() => { const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true }); if (!selected && !event.defaultPrevented) navigation.navigate(route.name, route.params); }}><PlayIcon name={item.icon} size={23} color={selected ? '#fff' : theme.muted} /><Text fontSize={12} fontW="600" color={selected ? '#fff' : theme.muted}>{item.label}</Text></Pressable>;
  })}</View>;
}
function MainTabs() {
  return <Tabs.Navigator screenOptions={{ headerShown: false }} tabBar={props => <FloatingTabs {...props} />}><Tabs.Screen name="Home" component={CameraHomeScreen} /><Tabs.Screen name="Rooms" component={RoomNavigation} /><Tabs.Screen name="Messages" component={MessagesScreen} /><Tabs.Screen name="Time" component={MemoriesScreen} /></Tabs.Navigator>;
}
function CaptureScreen({ route }: AppStackScreenProps<RouteName.KeoCapture>) { return <CameraHomeScreen roomId={route.params.roomId} />; }
function RoomScreen({ route }: AppStackScreenProps<RouteName.KeoRoomDetail>) {
  const { rooms } = useKeoStore();
  if (!rooms.some((room) => room.id === route.params.roomId)) return <KeoHomeScreen />;
  return (
    <RoomProvider roomId={route.params.roomId}>
      <KeoRoomDetailScreen />
    </RoomProvider>
  );
}
export function AppNavigator() {
  return (
    <OnlineProvider><PhotoProvider><NavigationContainer
      ref={navigationRef}
      theme={{
        ...DefaultTheme,
        colors: { ...DefaultTheme.colors, background: theme.background, primary: theme.green },
      }}
    >
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name={RouteName.KeoMain} component={MainTabs} />
        <Stack.Screen name={RouteName.KeoCapture} component={CaptureScreen} />
        <Stack.Screen name={RouteName.KeoPhotoPreview} component={PhotoPreviewScreen} />
        <Stack.Screen name={RouteName.KeoPhotoDetail} component={PhotoDetailScreen} />
        <Stack.Screen name={RouteName.KeoHome} component={KeoHomeScreen} />
        <Stack.Screen name={RouteName.KeoRoomDetail} component={RoomScreen} />
      </Stack.Navigator>
    </NavigationContainer></PhotoProvider></OnlineProvider>
  );
}
const styles = StyleSheet.create({ tabs: { position: 'absolute', left: 24, right: 24, maxWidth: 390, alignSelf: 'center', borderRadius: 36, flexDirection: 'row', backgroundColor: '#fffefaf7', borderWidth: 1, borderColor: theme.border, padding: 5, elevation: 5, shadowColor: theme.ink, shadowOpacity: 0.1, shadowRadius: 14, shadowOffset: { width: 0, height: 5 } }, tab: { flex: 1, minHeight: 58, borderRadius: 30, justifyContent: 'center', alignItems: 'center', gap: 2, paddingVertical: 6 }, activeTab: { backgroundColor: theme.green } });
