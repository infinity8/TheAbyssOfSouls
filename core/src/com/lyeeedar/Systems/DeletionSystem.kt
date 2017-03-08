package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Level.World
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random

class DeletionSystem : AbstractSystem(Family.all(MarkedForDeletionComponent::class.java).get())
{
	val deathEffect = AssetManager.loadParticleEffect("Death")

	override fun doUpdate(deltaTime: Float)
	{
		deletedEntities.clear()

		for (entity in entities)
		{
			processEntity(entity)
		}
	}

	val deletedEntities = ObjectSet<Entity>()

	fun processEntity(entity: Entity)
	{
		if (deletedEntities.contains(entity)) return
		deletedEntities.add(entity)

		val pos = entity.pos()
		if (pos != null && pos.tile != null)
		{
			for (x in 0..pos.size-1)
			{
				for (y in 0..pos.size-1)
				{
					val tile = pos.tile!!.level.getTile(pos.tile!!, x, y) ?: continue
					if (tile.contents[pos.slot] == entity) tile.contents[pos.slot] = null
				}
			}
		}

		val drop = entity.drop()
		if (drop != null)
		{
			for (dropdata in drop.drops)
			{
				if (Random.random() <= dropdata.chance)
				{
					DropComponent.dropTo(pos.tile!!, pos.tile!!, dropdata.item)
				}
			}
		}

		val trail = entity.trailing()
		if (trail != null)
		{
			for (e in trail.entities.toList())
			{
				processEntity(e)
			}
		}

		if (entity.task() != null)
		{
			val effect = deathEffect.copy()
			val effectEntity = Entity()

			effectEntity.add(RenderableComponent(effect))
			effectEntity.add(PositionComponent())
			val effectPos = effectEntity.pos()!!

			effectPos.position = pos.position
			effectPos.slot = pos.slot

			Global.engine.addEntity(effectEntity)
		}

		engine.removeEntity(entity)

		if (entity == level!!.player)
		{
			val current = World.world.currentLevel
			val next = current.connections["death"]

			World.world.changeLevel(next)
		}
	}
}
