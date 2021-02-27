package se.kth.id2203.detector

import se.kth.id2203.broadcast._
import se.kth.id2203.detector.EPFD._
import se.kth.id2203.networking.{NetAddress, NetMessage}
import se.sics.kompics.{Init, KompicsEvent}
import se.sics.kompics.network.{Address, Network, Transport}
import se.sics.kompics.sl.{ComponentDefinition, _}
import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.Await
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}

case class Suspect(process: NetAddress) extends KompicsEvent;
case class Restore(process: NetAddress) extends KompicsEvent;
case class Monitor(processes: Set[NetAddress]) extends KompicsEvent;

class EventuallyPerfectFailureDetector extends Port {
  indication[Suspect];
  indication[Restore];
  request[Monitor];
}

case class HeartbeatReply(seq: Int) extends KompicsEvent;
case class HeartbeatRequest(seq: Int) extends KompicsEvent;

object EPFD {
  case class EPFDInit(selfAddr: NetAddress) extends Init[EPFD];
}

class EPFD(init: EPFDInit) extends ComponentDefinition {

  //******* Ports ******
  val timer = requires[Timer];
  val pLink = requires(PerfectLink);
  val epfd  = provides[EventuallyPerfectFailureDetector];

  //******* Fields ******
  val self  = init.selfAddr;
  var topology = Set[NetAddress]();
  val delta = cfg.getValue[Long]("id2203.project.keepAlivePeriod");

  var period = delta;
  var alive = Set[NetAddress]();
  var suspected = Set[NetAddress]();
  var seqnum = 0;

  //******* Handlers ******
  def startTimer(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(period);
    scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }

  ctrl uponEvent {
    case _: Start => {
    }
  }

  epfd uponEvent {
    case Monitor(nodes) => {
      startTimer(period)
      topology = nodes;
      alive = nodes;
      suspected = Set[NetAddress]();
      seqnum = 0;
    }
  }

  timer uponEvent {
    case CheckTimeout(_) => {
      if (alive.intersect(suspected).nonEmpty) {
        period += delta
      }

      seqnum = seqnum + 1;

      for (p <- topology) {
        if (!alive.contains(p) && !suspected.contains(p)) {
          log.info(s"$self suspects $p");
          suspected += p
          trigger(Suspect(p) -> epfd)
        } else if (alive.contains(p) && suspected.contains(p)) {
          suspected = suspected - p;
          log.info(s"$self restored $p")
          trigger(Restore(p) -> epfd);
        }
        trigger(PL_Send(p, HeartbeatRequest(seqnum)) -> pLink);
      }
      alive = Set[NetAddress]();
      startTimer(period);
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, HeartbeatRequest(seq)) => {
      trigger(PL_Send(src, HeartbeatReply(seq)) -> pLink);
    }
    case PL_Deliver(src, HeartbeatReply(seq)) => {
      if (seq == seqnum || suspected.contains(src)) alive = alive + src
    }
  }
};