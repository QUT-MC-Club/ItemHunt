package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;

public class ItemHuntWaitingPhase extends ItemHuntPhase {

    public ItemHuntWaitingPhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        GameWaitingLobby.addTo(activity, game.config().playerConfig());

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayers);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private void start() {
        var pos = game.spawnPos().toCenterPos();
        var worldBorder = game.world().getWorldBorder();
        worldBorder.setCenter(pos.getX(), pos.getZ());
        worldBorder.setSize(9);
        worldBorder.setWarningBlocks(-100);
    }

    private JoinAcceptorResult acceptPlayers(JoinAcceptor offer) {
        return offer.teleport(game.world(), game.spawnPos().toCenterPos()).thenRunForEach((player, intent) -> {
            player.changeGameMode(intent == JoinIntent.SPECTATE ? GameMode.SPECTATOR : GameMode.ADVENTURE);
            player.sendMessage(ItemHuntTexts.description(game), false);
        });
    }

    private void addPlayer(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(game.world().getWorldBorder()));
    }

    private GameResult requestStart() {
        var activePhase = new ItemHuntActivePhase(game);
        game.gameSpace().setActivity(activePhase::setup);
        return GameResult.ok();
    }
}
