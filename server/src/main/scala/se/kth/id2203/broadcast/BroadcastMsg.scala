package se.kth.id2203.broadcast

import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{ComponentDefinition => _, Port => _, KompicsEvent}
import se.kth.id2203.networking._

case class BROADCAST_Test(msg: String) extends KompicsEvent;
case class BROADCAST_WITH_SOURCE(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;