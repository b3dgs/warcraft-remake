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
package com.b3dgs.warcraft.action;

import java.util.List;

import com.b3dgs.lionengine.Align;
import com.b3dgs.lionengine.Media;
import com.b3dgs.lionengine.Medias;
import com.b3dgs.lionengine.game.Bar;
import com.b3dgs.lionengine.game.Configurer;
import com.b3dgs.lionengine.game.feature.Factory;
import com.b3dgs.lionengine.game.feature.Featurable;
import com.b3dgs.lionengine.game.feature.Services;
import com.b3dgs.lionengine.game.feature.Setup;
import com.b3dgs.lionengine.game.feature.collidable.selector.Selectable;
import com.b3dgs.lionengine.game.feature.producible.Producer;
import com.b3dgs.lionengine.game.feature.producible.Producible;
import com.b3dgs.lionengine.game.feature.producible.ProducibleListener;
import com.b3dgs.lionengine.game.feature.producible.ProducibleListenerVoid;
import com.b3dgs.lionengine.game.feature.tile.map.pathfinding.CoordTile;
import com.b3dgs.lionengine.game.feature.tile.map.pathfinding.MapTilePath;
import com.b3dgs.lionengine.game.feature.tile.map.pathfinding.Pathfindable;
import com.b3dgs.lionengine.graphic.Graphic;
import com.b3dgs.lionengine.graphic.drawable.Image;
import com.b3dgs.warcraft.Player;
import com.b3dgs.warcraft.Util;
import com.b3dgs.warcraft.constant.Constant;
import com.b3dgs.warcraft.object.CostConfig;
import com.b3dgs.warcraft.object.feature.EntitySfx;

/**
 * Produce button action.
 */
public class ProduceButton extends ActionModel
{
    private static final int PROGRESS_X = 2;
    private static final int PROGRESS_Y = 107;
    private static final int PROGRESS_WIDTH = 62;
    private static final int PROGRESS_HEIGHT = 5;
    private static final int PROGRESS_OFFSET = 2;
    private static final int TEXT_WOOD_X = 220;
    private static final int TEXT_GOLD_X = 265;
    private static final int TEXT_Y = 193;
    private static final int TEXT_OFFSET_X = 17;
    private static final String ATT_MEDIA = "media";

    /**
     * Create progress bar.
     * 
     * @return The created progress bar.
     */
    private static Bar createBar()
    {
        final Bar bar = new Bar(PROGRESS_WIDTH, PROGRESS_HEIGHT);
        bar.setLocation(PROGRESS_X + PROGRESS_OFFSET, PROGRESS_Y + PROGRESS_OFFSET);
        bar.setWidthPercent(0);
        bar.setHeightPercent(com.b3dgs.lionengine.Constant.HUNDRED);
        bar.setColorForeground(Constant.COLOR_HEALTH_GOOD);
        return bar;
    }

    private final Image wood = Util.getImage("wood.png", TEXT_WOOD_X, TEXT_Y - 2);
    private final Image gold = Util.getImage("gold.png", TEXT_GOLD_X, TEXT_Y - 1);
    private final Image progressBackground = Util.getImage("progress.png", PROGRESS_X, PROGRESS_Y);
    private final Image progressPercent = Util.getImage("progress_percent.png", PROGRESS_X, PROGRESS_Y);
    private final Bar progressBar = createBar();
    private final CostConfig config;

    private boolean producing;

    /**
     * Create build button action.
     * 
     * @param services The services reference.
     * @param setup The setup reference.
     */
    public ProduceButton(Services services, Setup setup)
    {
        super(services, setup);

        final Media target = Medias.create(setup.getText(ATT_MEDIA));
        final Factory factory = services.get(Factory.class);
        final Player player = services.get(Player.class);

        config = CostConfig.imports(new Configurer(target));

        actionable.setAction(() ->
        {
            if (player.isAvailableFood()
                && player.isAvailableWood(config.getWood())
                && player.isAvailableGold(config.getGold()))
            {
                player.decreaseWood(config.getWood());
                player.decreaseGold(config.getGold());

                final Featurable entity = factory.create(target);
                final Producible producible = entity.getFeature(Producible.class);
                // TODO clear listener once production done
                producible.addListener(createListener(producible));

                final List<Selectable> selection = selector.getSelection();
                final int n = selection.size();
                for (int i = 0; i < n; i++)
                {
                    final Producer producer = selection.get(i).getFeature(Producer.class);
                    producer.addToProductionQueue(entity);
                }
            }
        });
    }

    /**
     * Create production listener.
     * 
     * @param producible The producible in production.
     * @return The created listener.
     */
    private ProducibleListener createListener(Producible producible)
    {
        return new ProducibleListenerVoid()
        {
            @Override
            public void notifyProductionStarted(Producer producer)
            {
                producible.getFeature(EntitySfx.class).onStarted();
                producing = true;
            }

            @Override
            public void notifyProductionProgress(Producer producer)
            {
                progressBar.setWidthPercent(producer.getProgressPercent());
            }

            @Override
            public void notifyProductionEnded(Producer producer)
            {
                teleportOutside(producible, producer);
                progressBar.setWidthPercent(0);
                producible.getFeature(EntitySfx.class).onProduced();
                producing = false;
            }
        };
    }

    /**
     * Teleport producer outside producible area.
     * 
     * @param producible The producible reference.
     * @param producer The producer to teleport.
     */
    private void teleportOutside(Producible producible, Producer producer)
    {
        final Pathfindable pathfindable = producible.getFeature(Pathfindable.class);
        final CoordTile coord = map.getFeature(MapTilePath.class)
                                   .getFreeTileAround(pathfindable, producer.getFeature(Pathfindable.class));
        pathfindable.setLocation(coord);
    }

    @Override
    public void render(Graphic g)
    {
        if (producing)
        {
            progressBackground.render(g);
            progressBar.render(g);
            progressPercent.render(g);
        }

        if (actionable.isOver())
        {
            text.draw(g, TEXT_WOOD_X + TEXT_OFFSET_X, TEXT_Y, Align.LEFT, String.valueOf(config.getWood()));
            text.draw(g, TEXT_GOLD_X + TEXT_OFFSET_X, TEXT_Y, Align.LEFT, String.valueOf(config.getGold()));
            wood.render(g);
            gold.render(g);
        }
    }
}
