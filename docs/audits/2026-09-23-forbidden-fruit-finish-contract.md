# Forbidden Fruit completion hook

Status: source implementation only; no build or client run in this lane.

The 592 `EventHandler.onEatEvent(PlayerUseItemEvent.Finish)` checks an equipped
`ItemFoodAmulet`, the server side, and the selected item's use action EAT **or
DRINK**. It does not require a stack-count decrease, so a creative completion
is still eligible. The apple branch grants regeneration for 7,200 ticks at
amplifier 1; other items use the legacy item-name-seeded positive-effect roll.

In 1.21.1, `LivingEntity.completeUsingItem` verifies the active hand/stack,
calls `ItemStack.finishUsingItem`, and then stops use. The new MixinExtras
`@WrapOperation` targets that invocation, captures the pre-finish `UseAnim`,
copies the pre-finish EAT/DRINK stack, calls
the original operation, and only then forwards a completed server-player use
to `AuraItems.onFoodFinished`. This removes the pending-UUID map, count-change
polling, and elapsed-time creative guess. Cancellation or dropping a stack
without completing use does not call this hook.

Eligibility now requires the captured animation to be `UseAnim.EAT` or
`UseAnim.DRINK`, an alive nonspectator server player, and the actually equipped
amulet. It does not require `DataComponents.FOOD` or a count decrease. Potion
and milk completion therefore qualify, including creative completion. The
pre-finish copy supplies the consumed item's identity even when the original
operation empties the stack or returns a bottle/bucket. Effects run after the
original operation returns normally, including after milk clears old effects.
An exception from the original does not run the effect; canceled or interrupted
use never reaches this finish invocation. All other use animations are rejected.

Focused checks: `ForbiddenFruitCompletionTest` covers food, non-FOOD potion and
milk, all other animation rejections, alive/spectator/equipped gates, captured
animation independence from the consumed stack, source ordering of the original
call and post-completion hook, and mixin-list registration. These are eligibility
and source-wiring tests, not a live cancellation or potion-effect integration test.
The updated tests have not been run in this lane. Parent live acceptance still
needs food, potion and milk completion, creative completion, canceled use, and
moving/dropping the used stack, with no effect in canceled cases. This closes the
source-level EAT/DRINK eligibility gap; it does not claim full runtime parity.
