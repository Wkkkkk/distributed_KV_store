package se.kth.id2203.broadcast

import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{ComponentDefinition => _, Port => _, KompicsEvent}
import se.kth.id2203.networking._

import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer

class TotalOrderBroadcast extends Port {
  indication[TO_Deliver];
  request[TO_Broadcast];
}

case class TO_Deliver(source: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class TO_Broadcast(payload: KompicsEvent) extends KompicsEvent;
