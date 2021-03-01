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

import se.kth.id2203.networking._;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.sl._;
import se.sics.kompics.network.Network;
import scala.collection.mutable


class KVService extends ComponentDefinition {

  //******* Ports ******
  val net = requires[Network];
  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val store = mutable.HashMap.empty[String, String];
  init(10);

  //******* Handlers ******
  net uponEvent {
    case NetMessage(header, op @ Get(key, _)) => {
      // trigger(NetMessage(self, header.src, op.response(OpCode.NotImplemented)) -> net);
      val result = store.get(key);
      log.info("Got operation {}! result is: {}", op, result);
      if(result.isDefined)
        trigger(NetMessage(self, header.src, op.response(OpCode.Ok, result)) -> net);
      else
        trigger(NetMessage(self, header.src, op.response(OpCode.NotFound, None)) -> net);
      log.info("send back to {}", header.src);
    }
    case NetMessage(header, op @ Put(key, value, _)) => {
      log.info("Got operation {}! Let's do it together :)", op);
//      trigger(NetMessage(self, header.src, op.response(OpCode.NotImplemented)) -> net);
      store += ( key -> value );
      trigger(NetMessage(self, header.src, op.response(OpCode.Ok, store.get(key))) -> net);
    }
  }

  def init(amount: Int): Unit = {
    for (i <- 0.to(amount) ) {
      store += ( (s"$i", "value" + i) )
    }
  }
}
