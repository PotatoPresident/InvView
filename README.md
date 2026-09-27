# InvView
Allows you to get the inventory and echest of other players

## Trinkets Updated support

With Trinkets Updated 4.1.1+26.2 installed, `/view trinket <target>` opens an online
or offline player's active trinket slots. InvView itself remains
optional on the client; the usual client requirements of Trinkets still apply.

The menu displays up to 18 slots per page, supports additional pages, and includes
a regular/cosmetic toggle when cosmetic slots are available. The exact slots depend
on the target player's active Trinkets configuration. If the slot structure or target
player entity changes while viewing, the menu closes and asks you to reopen it.
Switching pages or modes preserves the cursor stack and cancels any unfinished drag
operation.
