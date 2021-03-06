/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore;

import se.kth.id2203.broadcast._;
import se.kth.id2203.consensus._;
import se.kth.id2203.networking._;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.sl._;
import se.sics.kompics.network.Network;
import scala.collection.mutable


case class KVCommand(header: NetHeader, op: Operation) extends RSM_Command with KompicsEvent {
  override def isRead: Boolean = op.isInstanceOf[Get]
}

class KVService extends ComponentDefinition {
  //******* Ports ******
  val net = requires[Network];
  val rb = requires[ReliableBroadcast];
  val sc = requires[SequenceConsensus];

  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val store: collection.mutable.Map[String, String] = collection.mutable.Map.empty
  init(100)

  //******* Handlers ******
  net uponEvent {
    case NetMessage(header, op: Operation) => {
      trigger(RB_Broadcast(KVCommand(header, op)) -> rb)
    }
  }

  rb uponEvent {
    case RB_Deliver(src, kvc: KVCommand) => {
      trigger(SC_Propose(kvc) -> sc)
    }
  }

  sc uponEvent {
    case SC_Decide(KVCommand(header, op: Operation)) => {
      op match {
        case _: Get => {
          store.get(op.key) match {
            case Some(value) => trigger(NetMessage(self, header.src, op.response(OpCode.Ok, Some(value))) -> net)
            case None => trigger(NetMessage(self, header.src, op.response(OpCode.NotFound, None)) -> net)
          }
        }
        case p: Put => {
          store(p.key) = p.value
          trigger(NetMessage(self, header.src, op.response(OpCode.Ok, store.get(p.key))) -> net)
        }
        case c: Cas => {
          if (store.contains(c.key) ) {
            val result = store(c. key)
            if ( c.compareValue == result ) {
              store += (c.key -> c.setValue)
              trigger(NetMessage(self, header.src, op.response(OpCode.Ok, Some(result))) -> net);
            } else
              trigger(NetMessage(self, header.src, op.response(OpCode.ReferenceValuesIsNotCurrentValue, None)) -> net);
          } else  {
            trigger(NetMessage(self, header.src, op.response(OpCode.NotFound, None)) -> net);
          }
        }
      }
    }
  }

  def init(amount: Int): Unit = {
    for (i <- 0.to(amount) ) {
      store += ( (s"test$i", "value" + i) )
    }
  }
}
