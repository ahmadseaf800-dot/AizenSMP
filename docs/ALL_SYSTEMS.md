# AizenSMP - Complete Systems

This is the master checklist for the server.

## Core
- Persistent player data
- First join
- Join/quit messages
- MOTD
- Rules/help GUI
- Reload-safe storage
- Permissions and staff roles
- Admin audit logs

## Economy
- Balance
- Pay
- Transaction history
- Economy safeguards
- No negative balances
- Admin economy controls

## Shop
- GUI-first shop
- Categories and pages
- Buy/sell controls
- Sell Hand
- Sell All
- Quantity selection
- Confirmation menus
- Configurable prices
- No normal /buy command
- Shop/economy integration

## Orders
- Player buy orders
- Highest-price matching
- FIFO tie handling
- Partial fulfillment
- /sell matching orders
- Remaining quantity
- Expiration
- Cancellation and refunds
- Balance reservation
- Notifications

## Auction House
- /ah
- /ah sell <price>
- Browse/search/categories
- Confirmation GUI
- Green confirm / red cancel
- Shulker contents preview only for shulkers
- Listing cancellation
- Expiration
- Atomic transactions

## Trading
- /trade <player>
- Two-sided item and money GUI
- Ready/confirm stages
- Reset confirmations after changes
- Countdown
- Safe cancellation
- Atomic execution

## Teleport
- /tpa
- /tpahere
- /tpaccept
- /tpdeny
- /sethome
- /home
- /delhome
- Multiple homes
- /spawn
- /setspawn
- /rtp
- Cooldowns
- Safe destination checks

## Player settings
- TPA toggle
- Trade toggle
- Private message toggle
- AH notification toggle
- Order notification toggle
- Join/quit message toggle

## PvP
- PvP state
- Combat Tag
- Combat timer
- Combat logout handling
- Kill/death statistics
- Spawn PvP configuration
- Safe spawn protection

## Bounty/Wanted
- Bounty creation
- Bounty claiming
- Wanted list
- Notifications
- Anti-abuse checks
- Leaderboards

## Stats/Leaderboards
- Kills
- Deaths
- K/D
- Playtime
- Blocks broken/placed
- Mobs killed
- Balance
- Bounties
- Orders
- AH sales
- Top-player GUIs

## Crates/Keys
- Multiple key tiers
- Crate GUI
- Preview
- Rewards and chances
- Animations
- Broadcasts
- Key validation
- Anti-duplication
- Admin configuration

## Spawners
- Spawner GUI
- Configurable mobs
- Buy/sell
- Pickup rules
- Ownership
- Protection
- Admin controls
- Anti-duplication

## Kits/Rewards
- Starter kit
- Cooldowns
- One-time/cooldown protection
- Daily rewards
- Playtime rewards
- Voting rewards
- Milestones
- Achievements
- Streaks

## Player utilities
- Player warps
- Rules/info GUI
- Help GUI
- Announcements
- AFK
- Nick/display-name support
- Ignore
- Private messages
- Chat utilities

## Protection
- Anti-duplication
- Atomic economy transactions
- Item validation
- Trade locking
- AH locking
- Order balance reservation
- Restart-safe transactions
- Core protection
- Staff audit logs

## Anti-cheat/security
- Anti-Xray
- Server-side validation
- Staff X-Ray with separate permission
- Abuse detection hooks
- Suspicious transaction logging

## Admin/Staff
- Admin GUI
- Staff mode
- Vanish
- Freeze
- Spectate
- Teleport
- Inventory inspection
- Player information
- Economy controls
- Clear inventory
- Kick/ban/mute hooks
- Staff chat
- Audit logs

## Events
- Supply drops
- KOTH
- PvP events
- Timed rewards
- Announcements
- Event leaderboards
- Custom server events

## Spawn
A ready-to-use spawn is planned as a generated schematic/world asset with:
- Spawn platform
- Welcome area
- Shop area
- Crates/Keys area
- Warps/RTP board
- Rules/info area
- Safe PvP-free zone
- Staff/admin area
- Decorative paths and lighting

The spawn asset should be installed as a separate world or pasted with WorldEdit/FAWE, then /setspawn is run at the exact center.
