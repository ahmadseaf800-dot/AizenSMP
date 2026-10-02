# AizenSMP - Complete SMP Systems

## Core
- Persistent player data
- Economy / balance
- Spawn
- First-join setup
- Permissions and staff roles
- Server messages
- Reload-safe storage

## Classic-style Shop (highest priority)
The shop is GUI-first and must NOT depend on a normal /buy command.
- Main Shop GUI
- Categories
- Item pages
- Buy
- Sell
- Sell Hand
- Sell All
- Item prices
- Quantity selection
- Purchase confirmation
- Balance display
- Transaction messages
- Shop navigation/back buttons
- Configurable prices
- Economy integration
- Orders integration where appropriate

The exact historical DonutSMP shop layout, item list, prices, and behavior must be verified against reliable archived/documented sources before being represented as an exact historical recreation. The implementation will use original AizenSMP code and assets.

## Economy
- Balance
- Pay
- Transaction history
- Money formatting
- Economy safeguards
- Admin economy controls
- Negative-balance prevention

## Orders
- Create buy order
- Order GUI
- Highest-price matching
- FIFO when prices tie
- Partial fulfillment
- /sell matching orders
- Remaining quantity
- Expiration
- Cancellation
- Refunds
- Balance reservation
- Notifications
- Atomic transactions

## Auction House
- /ah
- /ah sell <price>
- Browse/search/categories
- Listing details
- Purchase confirmation
- Green confirm button
- Red cancel button
- Shulker-box contents preview only for shulker listings
- No contents preview for normal items
- Seller cancellation
- Expiration
- Transaction validation
- Anti-duplication

## Player Trade
- Trade request
- Accept/deny
- Two-sided trade GUI
- Ready/confirm states
- Final confirmation
- Cancellation
- Item and money validation
- Anti-duplication

## Teleport
- /tpa
- /tpahere
- /tpaccept
- /tpdeny
- /sethome
- /home
- /delhome
- Multiple homes
- Home limits
- /spawn
- /setspawn
- /rtp
- RTP cooldown
- Safe-location checks
- Teleport-request settings

## Settings
- /settings GUI
- TPA toggle
- Trade toggle
- Private-message toggle
- AH notification toggle
- Order notification toggle
- Join/leave message toggle
- Persistent per-player settings

## PvP
- PvP state
- Combat tag
- Combat timer
- PvP protection rules
- Combat logout handling
- Kill/death tracking
- Spawn PvP rules

## Bounty / Wanted
- Set bounty
- Claim bounty
- Wanted targets
- Bounty GUI
- Notifications
- Anti-abuse checks
- Leaderboards

## Stats / Leaderboards
- Kills
- Deaths
- K/D
- Playtime
- Blocks broken
- Blocks placed
- Mobs killed
- Balance
- Bounties
- Orders
- AH sales
- Top-player GUIs

## Crates
- Crate keys
- Crate GUI
- Preview
- Rewards
- Animation
- Key storage
- Anti-duplication
- Admin configuration

## Kits
Starter kit:
- Full Chainmail Armor
- Stone Sword
- Stone Pickaxe
- Stone Axe
- Stone Shovel
- 16 Bread
- Cooldown
- One-time/cooldown protection

## Player progression
- Playtime rewards
- Daily rewards
- Voting rewards
- Milestones
- Streaks
- Achievements

## SMP systems
- Player warps
- Rules/info GUI
- Help GUI
- Server announcements
- MOTD
- Join/quit messages
- First join
- AFK
- Nickname/display-name support
- Ignore system
- Private messages
- Social/chat utilities

## Protection / anti-abuse
- Anti-duplication
- Atomic economy transactions
- Item ownership validation
- Trade locking
- AH listing locking
- Order balance reservation
- Crash/restart-safe transactions
- Staff audit logs
- Core protection hooks

## Admin / Staff
- Admin GUI
- Staff mode
- Vanish
- Freeze
- Spectate
- Teleport
- Inventory inspection
- Player info
- Economy controls
- Clear inventory
- Kick/ban/mute hooks
- Staff chat
- Audit logs

## Events
- Custom events
- Supply drops
- KOTH
- PvP events
- Timed rewards
- Event announcements
- Event leaderboards

## Development order
1. Economy
2. Classic-style Shop
3. Orders + /sell
4. AH
5. Homes/RTP/Spawn
6. Trade
7. Settings
8. Stats/Leaderboards
9. PvP/Bounty/Wanted
10. Crates/Kits/Rewards
11. Protection
12. Admin/Staff
13. Events and additional SMP systems
