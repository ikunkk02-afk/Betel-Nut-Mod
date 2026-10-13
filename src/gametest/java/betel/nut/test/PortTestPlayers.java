package betel.nut.test;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
public final class PortTestPlayers {
 public static ServerPlayer join(GameTestHelper h){
  var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"port-mock"),false);
  var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
  var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
  new io.netty.channel.embedded.EmbeddedChannel(connection);
  net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
  h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
  var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;
 }
}
