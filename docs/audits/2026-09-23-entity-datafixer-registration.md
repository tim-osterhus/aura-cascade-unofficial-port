# Entity Data Fixer Registration

The 1.21.1 startup warning `No data fixer registered for aura:fairy` and its
`aura:miner_explosion` counterpart came from passing those new IDs to vanilla
`EntityType.Builder.build(String)`. That method calls
`Util.fetchChoiceType(References.ENTITY_TREE, id)` when the entity is saveable.
Neither entity has a historical 1.21.1 schema to migrate through DFU.

Installed Fabric API 0.116.11 provides the injected no-ID
`EntityType.Builder.build()` method. Its implementation calls vanilla
`build(null)` and skips only the choice-type lookup for the null ID. Both
registries now use that path while retaining their registry IDs, saveability,
and custom NBT read/write methods. `noSave()` would suppress persistence and is
not an acceptable fix for either entity.

This does not add a migration for legacy Aura Cascade worlds or for any future
schema change. A future saved-entity format change still needs an explicit
migration decision. Verification after the next serialized build: start a world,
confirm neither startup warning appears, save/reload with a fairy and a moving
miner entity present, and confirm their saved state still restores.
