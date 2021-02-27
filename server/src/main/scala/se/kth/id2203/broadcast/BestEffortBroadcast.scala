package se.kth.id2203.broadcast

import se.kth.id2203.networking.NetAddress
import se.sics.kompics.KompicsEvent
import se.sics.kompics.network.Address
import se.sics.kompics.sl.{ComponentDefinition, Init, Port, handle}

import scala.collection.immutable.Set

case class BROADCAST_Test(msg: String) extends KompicsEvent;
case class BEB_Deliver(src: Address, payload: KompicsEvent) extends KompicsEvent;
case class BEB_Broadcast(payload: KompicsEvent) extends KompicsEvent;
case class Set_Topology(topology: Set[NetAddress]) extends KompicsEvent;

class BestEffortBroadcast extends Port {
  indication[BEB_Deliver];
  request[BEB_Broadcast];
  request[Set_Topology];
}

class BasicBroadcast extends ComponentDefinition {

  //******* Ports ******
  val pLink = requires(PerfectLink);
  val beb = provides[BestEffortBroadcast];

  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  var topology = Set[NetAddress]();

  //******* Handlers ******
  beb uponEvent {
    case x: BEB_Broadcast => {
      //println( s"sending broadcast with $topology")
      for (q <- topology) {
        trigger(PL_Send(q, x) -> pLink)
      }
    }
    case Set_Topology(setNodes) => {
      topology = setNodes
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, BEB_Broadcast(payload)) => {
      trigger(BEB_Deliver(src, payload) -> beb)
    }
  }
}