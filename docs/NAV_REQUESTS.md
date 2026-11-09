# Navigation requests (design-gap work packages)

Only WP-1 (or the coordinator at merge) edits `navigation/NeerlyNavHost.kt` / `Routes`.
Other packages add their composables with callback parameters and list what they need here.

## Provided by WP-1

| Route | Screen | How to use |
|---|---|---|
| `Routes.legal(LegalDoc.Terms / .Privacy)` | `feature/legal/LegalScreen` | Already linked from Welcome ("Terms", "Privacy Policy") and Profile ("Terms & privacy"). |
| `Routes.LocationDenied` | `feature/auth/LocationDeniedScreen` | Open it when location permission is refused. It pops back and sets `"location_choice"` on the previous entry's `savedStateHandle`: `"granted"` (try GPS again) or `"typed"` (show the typed-address form). |

## Requests

### WP-2 → WP-1: open LocationDenied from the address form — **done (WP-2, wired in NeerlyNavHost)**
- `AddressFormScreen` adds `onLocationDenied: () -> Unit = {}` and calls it where it now calls `vm.locationDenied()` after a refused permission request.
- It reads the result: `nav.currentBackStackEntry?.savedStateHandle?.getStateFlow("location_choice", "")` (or a param the coordinator passes in) and retries GPS on `"granted"`.
- WP-1/coordinator then passes `onLocationDenied = { nav.navigate(Routes.LocationDenied) }` at `Routes.Address`, `Routes.AddressNew` and `Routes.AddressEdit`.

### WP-2 (Address & maps) — no new routes
- The map pin (Address, AddressPin, AddressPinEdit), search (AddressAdd) and typed path (AddressType) are inline in `AddressFormScreen`, which already serves `Routes.Address`, `Routes.AddressNew` and `Routes.AddressEdit`.
- The shop pin (VShopPin) is inline in the seller wizard's shop step.
- `Routes.Address/AddressNew/AddressEdit` pass `onLocationDenied = { nav.navigate(Routes.LocationDenied) }` and read `"location_choice"` from their own `savedStateHandle` (`granted` → GPS again, `typed` → focus the search box).
