package se.kth.id2203.consensus;

import se.kth.id2203.broadcast._
import se.kth.id2203.networking.NetAddress
import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{KompicsEvent, ComponentDefinition => _, Port => _}

import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}

case class Prepare(proposalBallot: (Int, Int)) extends KompicsEvent;
case class Promise(promiseBallot: (Int, Int), acceptedBallot: (Int, Int), acceptedValue: Option[Any]) extends KompicsEvent;
case class Accept(acceptBallot: (Int, Int), proposedValue: Any) extends KompicsEvent;
case class Accepted(acceptedBallot: (Int, Int)) extends KompicsEvent;
case class Nack(ballot: (Int, Int)) extends KompicsEvent;
case class Decided(decidedValue: Any) extends KompicsEvent;

case class C_Propose(value: Any) extends KompicsEvent;
case class C_Decide(value: Any) extends KompicsEvent;

class Consensus extends Port {
  request[C_Propose];
  indication[C_Decide];
}

class Paxos(paxosInit: Init[Paxos]) extends ComponentDefinition {

  implicit def addComparators[A](x: A)(implicit o: math.Ordering[A]): o.OrderingOps = o.mkOrderingOps(x);

  def toRank(addr : NetAddress) : Int = {
    return addr.getIp().getAddress()(3).toInt;
  }

  //Port Subscriptions for Paxos

  val consensus = provides[Consensus];
  val beb = requires[BestEffortBroadcast];
  val plink = requires[PerfectLink];

  //Internal State of Paxos
  val (rank, numProcesses) = paxosInit match {
    case Init(s: NetAddress, qSize: Int) => (toRank(s), qSize)
  }

  //Proposer State
  var round = 0;
  var proposedValue: Any = None;
  var promises: ListBuffer[((Int, Int), Option[Any])] = ListBuffer.empty;
  var numOfAccepts = 0;
  var decided = false;

  //Acceptor State
  var promisedBallot = (0, 0);
  var acceptedBallot = (0, 0);
  var acceptedValue: Option[Any] = None;

  def propose() = {
    if (!decided) {
      round += 1;
      numOfAccepts = 0;
      promises = ListBuffer.empty;

      trigger(BEB_Broadcast(Prepare(round, rank)) -> beb);
    }
  }

  consensus uponEvent {
    case C_Propose(value) => {
      proposedValue = value;
      propose();
    }
  }


  beb uponEvent {

    case BEB_Deliver(src, prep: Prepare) => {
      val ballot = prep.proposalBallot;
      if(promisedBallot < ballot) {
        promisedBallot = ballot;
        trigger(PL_Send(src, Promise(promisedBallot, acceptedBallot, acceptedValue)) -> plink);
      } else {
        trigger(PL_Send(src, Nack(ballot)) -> plink);
      }
    };

    case BEB_Deliver(src, acc: Accept) => {
      val ballot = acc.acceptBallot;
      val proposedValue = acc.proposedValue;
      if(promisedBallot <= ballot) {
        promisedBallot = ballot;
        acceptedBallot = ballot;
        acceptedValue  = Option(proposedValue);
        trigger(PL_Send(src, Accepted(ballot)) -> plink);
      } else {
        trigger(PL_Send(src, Nack(ballot)) -> plink);
      }
    };

    case BEB_Deliver(src, dec : Decided) => {
      if(!decided) {
        val v = dec.decidedValue;
        trigger(C_Decide(v) -> consensus);
        decided = true;
      }
    }
  }

  plink uponEvent {

    case PL_Deliver(src, prepAck: Promise) => {
      if ((round, rank) == prepAck.promiseBallot) {
        promises += ((prepAck.acceptedBallot, prepAck.acceptedValue));
        if(promises.length == (numProcesses + 2)/2) {

          var (maxBallot, value) = promises.maxBy(_._1);
          if(value.isDefined) {
            proposedValue = value.get;
          }
          trigger(BEB_Broadcast(Accept((round, rank), proposedValue)) -> beb);
        }

      }
    };

    case PL_Deliver(src, accAck: Accepted) => {
      if ((round, rank) == accAck.acceptedBallot) {
        numOfAccepts += 1;
        if(numOfAccepts == (numProcesses + 2)/2) {
          trigger(BEB_Broadcast(Decided(proposedValue)) -> beb);
        }
      }
    };

    case PL_Deliver(src, nack: Nack) => {
      if ((round, rank) == nack.ballot) {
        propose();
      }
    }
  }


};