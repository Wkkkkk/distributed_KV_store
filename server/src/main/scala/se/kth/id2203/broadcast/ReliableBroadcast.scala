package se.kth.id2203.broadcast

import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{ComponentDefinition => _, Port => _, KompicsEvent}
import se.kth.id2203.networking._

import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer

class ReliableBroadcast extends Port {
  indication[RB_Deliver];
  request[RB_Broadcast];
}

case class RB_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class RB_Broadcast(payload: KompicsEvent) extends KompicsEvent;

class EagerReliableBroadcast extends ComponentDefinition {

  //******* Ports ******
  val beb = requires[BestEffortBroadcast];
  val rb = provides[ReliableBroadcast];

  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val delivered = collection.mutable.Set[KompicsEvent]();

  //******* Handlers ******
  rb uponEvent {
    case x@RB_Broadcast(payload) => {
      trigger(BEB_Broadcast(BROADCAST_WITH_SOURCE(self, payload)) -> beb)
    }
  }

  beb uponEvent {
    case BEB_Deliver(_, data@BROADCAST_WITH_SOURCE(origin, payload)) => {
      if(!delivered.contains(data)){
        delivered.add(data)
        trigger(RB_Deliver(origin, payload) -> rb)
        trigger(BEB_Broadcast(data) -> beb)
      }
    }
  }
}