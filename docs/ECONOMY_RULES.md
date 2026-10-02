# AizenSMP Economy Rules

## Shop
The main shop is GUI-based and should follow the classic Donut-style flow.
Do NOT add a player command named /buy for normal shop purchases.

## Auction House
### /ah sell <price>
Lists the item currently held by the player for the specified price.

Example:
- Player holds 32 diamonds.
- Player runs /ah sell 5000.
- The 32 diamonds are listed for 5000 total.

The AH should support:
- Listing
- Browsing
- Buying through the AH GUI
- Seller cancellation
- Expiration
- Balance validation
- Item-return handling
- Anti-duplication checks

## Orders
Orders are player buy requests for a specific item and amount at a specific unit price.

Example:
- Player creates an order for 100 of an item.
- Another player sells 5 of that item using /sell.
- The 5 items are immediately matched against the best available buy order.
- The buyer receives 5 items.
- The seller receives payment for 5 items.
- The order remains active with 95 items requested.

## /sell
/sell is NOT a generic shop command.

When a player runs /sell with items:
1. Find the highest-priced compatible active buy order.
2. Match as many items as possible.
3. Pay the seller using the order's unit price.
4. Deliver the sold items to the order owner.
5. Reduce the order amount by the amount sold.
6. Keep the order active when its remaining amount is greater than zero.
7. If the order reaches zero, complete and remove it.
8. If multiple orders have the same unit price, use the oldest active order first.

Example:
Order: 100 items at 20 each.
Seller uses /sell with 5 items.
Result:
- Seller gets 100 currency.
- Buyer receives 5 items.
- Order has 95 remaining.
- The buyer does not need to recreate the order.

## Important
All order and AH operations must be atomic and protected against duplication:
- Remove/lock items before completing a sale.
- Validate the buyer's balance before fulfilling an order.
- Refund correctly when an order is cancelled.
- Never create payment or item copies during partial fulfillment.
