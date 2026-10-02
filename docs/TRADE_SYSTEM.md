# AizenSMP - Player Trade

## GUI
- /trade <player>
- Two-sided trading GUI.
- Items and money can be offered.
- Ready -> Confirm Trade flow.
- Any change to items or money resets both Ready states.
- Final confirmation uses a short countdown.
- Disconnect, invalid inventory state or failed validation cancels safely.

## Protection
- Lock both trade inventories during execution.
- Validate every item and money amount immediately before completion.
- Execute the exchange atomically.
- Return all items on cancellation.
- Never trust client-side GUI state.
