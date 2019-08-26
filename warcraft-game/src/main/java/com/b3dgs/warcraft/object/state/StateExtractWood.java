/*
 * Copyright (C) 2013-2019 Byron 3D Games Studio (www.b3dgs.com) Pierre-Alexandre (contact@b3dgs.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.b3dgs.warcraft.object.state;

import com.b3dgs.lionengine.Animation;
import com.b3dgs.lionengine.Animator;
import com.b3dgs.lionengine.AnimatorFrameListener;
import com.b3dgs.lionengine.game.feature.Animatable;
import com.b3dgs.warcraft.Player;
import com.b3dgs.warcraft.object.EntityModel;
import com.b3dgs.warcraft.object.State;
import com.b3dgs.warcraft.object.feature.EntitySfx;
import com.b3dgs.warcraft.object.feature.EntityStats;

/**
 * Extract wood state implementation.
 */
final class StateExtractWood extends State
{
    private final Animator animator = model.getSurface();
    private final EntitySfx sfx = model.getFeature(EntitySfx.class);
    private final AnimatorFrameListener listener;
    private boolean cut;

    /**
     * Create the state.
     * 
     * @param model The model reference.
     * @param animation The animation reference.
     */
    StateExtractWood(EntityModel model, Animation animation)
    {
        super(model, animation);

        final Animatable animatable = model.getFeature(Animatable.class);
        listener = frame ->
        {
            if (!cut && animatable.getFrame() == animation.getLast())
            {
                cut = true;
                sfx.onAttacked();
            }
            else if (animatable.getFrame() == animation.getFirst())
            {
                cut = false;
            }
        };

        final EntityStats stats = model.getFeature(EntityStats.class);

        addTransition(StateCarryWood.class, () -> Player.isWood(model.getCarryResource()));
        addTransition(StateDie.class, () -> stats.getHealthPercent() == 0);
        addTransition(StateIdle.class, () -> !model.isMoveStarted() && !model.isGotoResource());
        addTransition(StateWalk.class,
                      () -> model.isMoveStarted()
                            && model.getExtractResource() == null
                            && model.getCarryResource() == null);
    }

    @Override
    public void enter()
    {
        super.enter();

        animator.addListener(listener);
        cut = false;
    }

    @Override
    public void exit()
    {
        super.exit();

        animator.removeListener(listener);
    }
}
