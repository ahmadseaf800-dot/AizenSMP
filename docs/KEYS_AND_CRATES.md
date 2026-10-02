# AizenSMP - Keys & Crates

## Keys
- Physical key items with unique identifiers.
- Multiple tiers: Common, Rare, Epic, Legendary and event keys.
- Keys cannot be duplicated by normal item copying.
- GUI key inventory/count system.
- Admin key give/remove controls.
- Optional key conversion/exchange.

## Crates
- Main crate GUI.
- Crate preview GUI.
- Configurable rewards and chances.
- Key requirement before opening.
- Opening animation and reward message.
- Rewards can include money, items, keys, spawners and kits.
- Broadcasts for rare rewards.
- Cooldowns where required.
- Admin create/edit/reload controls.
- Anti-duplication and atomic reward delivery.

## Crate opening flow
1. Player interacts with crate.
2. Server validates crate and key.
3. One key is reserved/removed.
4. Reward is selected.
5. Reward is delivered only after validation.
6. Failed delivery returns the key/reward safely.
7. The transaction is logged.
