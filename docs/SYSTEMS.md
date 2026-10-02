# AizenSMP - Systems Roadmap

This project aims to build a custom SMP experience inspired by classic economy/SMP mechanics.

## Priority 1 - Economy & Shop
- Balance / money
- Main shop GUI
- Buy / sell
- Item pricing
- Shop categories
- Custom item prices
- Sell hand / sell all
- Economy transaction messages
- Shop access and permissions

## Priority 2 - Player Trading & Market
- Auction House (AH)
- List item
- Cancel listing
- Buy listing
- Expiration
- Search/categories
- Price validation
- Seller/buyer balance handling
- Anti-duplication checks
- Player-to-player trading

## Priority 3 - Teleport & Homes
- TPA
- TPAHere
- TPAccept / TPDeny
- /sethome
- /home
- /delhome
- Multiple homes
- Home limits
- Spawn
- RTP
- Random teleport cooldown
- Teleport safety

## Priority 4 - Orders
- Player orders
- Buy orders
- Sell orders
- Order GUI
- Order expiration
- Automatic fulfillment
- Balance/item reservation
- Cancellation and refunds
- Anti-duplication validation

## Priority 5 - Player Utilities
- /settings
- Toggle teleport requests
- Toggle private messages
- Toggle trade requests
- Toggle auction notifications
- Toggle join/leave messages
- Personal preferences

## Priority 6 - Progression
- Statistics
- Playtime
- Kills/deaths
- Leaderboards
- Balance leaderboard
- Custom milestones
- Rewards

## Priority 7 - PvP & SMP Systems
- PvP status
- Combat tag
- Bounty
- Wanted
- Kill rewards
- Death handling
- Crates
- Kits
- Events

## Priority 8 - Administration & Protection
- Staff commands
- Permissions
- Logging
- Anti-abuse
- Anti-duplication
- Region protection
- Admin GUI
- Reload-safe persistent data

## Development rule
Each major system should be implemented as its own Skript module, tested, then connected to the economy/data layer.

The shop and economy are the first major systems to implement.
