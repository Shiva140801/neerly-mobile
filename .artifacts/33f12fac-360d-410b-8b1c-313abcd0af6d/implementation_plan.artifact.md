# Implementation Plan - Fix Role-Based Routing and Auth Flow

This plan addresses the gaps identified in the mobile app's authentication and role-management flows to align with the PRD and UX v2.0 specifications.

## Proposed Changes

### [Component] Authentication & Role Management

#### [MODIFY] [AuthRepository.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/data/auth/AuthRepository.kt)
- Update `sendDevOtp` and `verifyDevOtp` to accept a dynamic role instead of hardcoded `"CUSTOMER"`.
- Implement `switchRole(newRole: String)` that calls `NeerlyApi.switchRole` and updates `TokenStore`.
- Add a way to store/retrieve `grantedRoles` (e.g., in a new `SharedPreferences` or by extending `TokenStore`).

#### [MODIFY] [TokenStore.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/data/auth/TokenStore.kt)
- Add `grantedRoles` to `saveTokens` and store it as a string set or JSON string.
- Add `lastRolePreference` to remember the user's choice.

#### [MODIFY] [AuthViewModel.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/feature/auth/AuthViewModel.kt)
- Support a `role` intent throughout the login flow.
- Add `switchRole` and `selectRole` functions.

#### [NEW] [RolePickerScreen.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/feature/auth/RolePickerScreen.kt)
- Implement `S-AUTH-ROL-01` as per UX spec.
- Display cards for Customer, Vendor, Driver, and Admin.

#### [MODIFY] [SplashScreen.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/feature/auth/SplashScreen.kt)
- Update logic to handle multiple roles and saved preferences.
- Route to `RolePicker` or specific home screens based on `activeRole`.

#### [MODIFY] [NeerlyNavHost.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/navigation/NeerlyNavHost.kt)
- Add `Routes.RolePicker`.
- Update `WelcomeScreen` and `ProfileScreen` navigation logic.

#### [MODIFY] [ProfileScreen.kt](file:///Users/shivasai/Documents/water/neerly-mobile/app/src/main/java/com/neerly/mobile/feature/profile/ProfileScreen.kt)
- Add "Switch Role" button if the user has multiple roles.

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure no regressions in navigation or auth logic.
- (Optional) Add unit tests for `AuthRepository` role-switching logic.

### Manual Verification
1.  **Login as Customer**: Verify standard flow.
2.  **Register as Vendor**: Verify that tapping "Register as Vendor" starts the flow with the correct role.
3.  **Multi-role Splash**: Simulate a user with both CUSTOMER and VENDOR roles and verify the `RolePicker` appears if no preference is saved.
4.  **Role Switching**: Verify switching from Customer to Vendor profile and vice-versa.
