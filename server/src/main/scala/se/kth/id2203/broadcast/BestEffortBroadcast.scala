package se.kth.id2203.broadcast

import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{ComponentDefinition => _, Port => _, KompicsEvent}
import se.kth.id2203.networking._

import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer

class BestEffortBroadcast extends Port {
  indication[BEB_Deliver];
  request[BEB_Broadcast];
}

case class BEB_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class BEB_Broadcast(payload: KompicsEvent) extends KompicsEvent;

class BasicBroadcast extends ComponentDefinition {
  //ports
  val beb = provides[BestEffortBroadcast];
  val net = requires[Network];
  val topo = requires[Topology];

  //configuration
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  var topology: Set[NetAddress] = Set(self)

  //handlers
  beb uponEvent {
    case x: BEB_Broadcast => {
      for (p <- topology) {
        trigger( NetMessage(self, p, x) -> net )
      }
    }
  }

  net uponEvent {
    case NetMessage(header, BEB_Broadcast(payload)) => {
      trigger( BEB_Deliver(header.src, payload) -> beb )
    }
  }

  topo uponEvent {
    case PartitionTopology(nodes) => {
      topology = nodes
    }
  }
}