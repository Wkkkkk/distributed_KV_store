package se.kth.id2203.broadcast

import se.kth.id2203.broadcast.PerfectP2PLink._
import se.sics.kompics.{Init, KompicsEvent}
import se.sics.kompics.network.{Address, Network, Transport}
import se.sics.kompics.sl.{ComponentDefinition, _}
import java.net.{InetAddress, InetSocketAddress}

import se.kth.id2203.networking.{NetAddress, NetMessage}

import scala.concurrent.Await


case class PL_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class PL_Send(dest: NetAddress, payload: KompicsEvent) extends KompicsEvent;

class PerfectLink extends Port {
  indication[PL_Deliver];
  request[PL_Send];
}

object PerfectP2PLink {
  case class PerfectLinkInit(selfAddr: NetAddress) extends Init[PerfectP2PLink];
}

class PerfectP2PLink(init: PerfectLinkInit) extends ComponentDefinition {

  //******* Ports ******
  val pLink = provides[PerfectLink];
  val network = requires[Network];

  //******* Fields ******
  val self = init.selfAddr;

  //******* Handlers ******
  pLink uponEvent {
    case PL_Send(dest, payload) => {
      trigger(NetMessage(self, dest, payload) -> network);
    }
  }

  network uponEvent {
    case NetMessage(header, payload) => {
      trigger(PL_Deliver(header.src, payload) -> pLink);
    }
  }

}