# AizenSMP Auction House GUI

## Commands
- /ah
- /ah sell <price>
- /ah sell <price> (held item is listed)
- /ah cancel

## Purchase flow
When a player clicks an AH listing:
1. Open a confirmation GUI.
2. Show the item being purchased.
3. If the listed item is a shulker box, show its contents in the preview.
4. If it is not a shulker box, do not show a contents preview.
5. Provide exactly two confirmation controls:
   - Green stained glass pane = confirm purchase
   - Red stained glass pane = cancel
6. Confirming re-validates the listing, item, seller, buyer balance, and price before completing the transaction.
7. Cancelling closes the confirmation screen without changing the listing.

## Shulker preview
The preview is read-only. It must never remove, duplicate, or alter the contents of the listed shulker box.

## Safety
AH transactions must be atomic:
- Lock/validate the listing before payment.
- Remove the listing only after the item transfer succeeds.
- Charge the buyer only after all validations pass.
- Return payment/item correctly on failure.
- Never duplicate shulker contents.
