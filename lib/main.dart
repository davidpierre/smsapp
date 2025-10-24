import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'screens/home_screen.dart';
import 'screens/setup_screen.dart';
import 'providers/sms_provider.dart';

void main() {
  runApp(const ProviderScope(child: SmsApp()));
}

class SmsApp extends ConsumerWidget {
  const SmsApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp(
      title: 'SMS App',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.light,
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.dark,
        ),
      ),
      themeMode: ThemeMode.system,
      home: const AppInitializer(),
    );
  }
}

class AppInitializer extends ConsumerWidget {
  const AppInitializer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDefaultSmsApp = ref.watch(isDefaultSmsAppProvider);
    final hasPermissions = ref.watch(hasPermissionsProvider);

    return isDefaultSmsApp.when(
      data: (isDefault) {
        if (!isDefault) {
          return const SetupScreen();
        }
        
        return hasPermissions.when(
          data: (hasPerms) {
            if (!hasPerms) {
              return const SetupScreen();
            }
            return const HomeScreen();
          },
          loading: () => const _LoadingScreen(),
          error: (_, __) => const SetupScreen(),
        );
      },
      loading: () => const _LoadingScreen(),
      error: (_, __) => const SetupScreen(),
    );
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: CircularProgressIndicator(),
      ),
    );
  }
}
